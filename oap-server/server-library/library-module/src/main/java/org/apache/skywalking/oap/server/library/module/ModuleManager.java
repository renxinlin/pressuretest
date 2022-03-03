/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.library.module;

import java.util.*;

/**
 * The <code>ModuleManager</code> takes charge of all {@link ModuleDefine}s in collector.
 *
 * @author wu-sheng, peng-yongsheng
 */
public class ModuleManager implements ModuleDefineHolder {

    private boolean isInPrepareStage = true;
    // 所有启动的模块集合[一个模块对应一个工作的moduleProvider,一个moduleProvider对应多个service]
    private final Map<String, ModuleDefine> loadedModules = new HashMap<>();

    /**
     * Init the given modules
     */
    public void init(
        ApplicationConfiguration applicationConfiguration) throws ModuleNotFoundException, ProviderNotFoundException, ServiceNotProvidedException, CycleDependencyException, ModuleConfigException, ModuleStartException {
        String[] moduleNames = applicationConfiguration.moduleList();
        // spi 加载
        ServiceLoader<ModuleDefine> moduleServiceLoader = ServiceLoader.load(ModuleDefine.class);
        ServiceLoader<ModuleProvider> moduleProviderLoader = ServiceLoader.load(ModuleProvider.class);


        // yml中配置了相关module   spi中配置了相关module模块
        // 取两者的交集
        // 并且根据spi得到的moduleProviderLoader 处理loadedProvider
        LinkedList<String> moduleList = new LinkedList<>(Arrays.asList(moduleNames));
        for (ModuleDefine module : moduleServiceLoader) {
            for (String moduleName : moduleNames) {
                if (moduleName.equals(module.name())) {
                    ModuleDefine newInstance;
                    try {
                        newInstance = module.getClass().newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new ModuleNotFoundException(e);
                    }
                    // 会创建 Module 对应的 ModuleProvider 。 注意: yml中一个Module[一级]只能配置一个ModuleProvider[二级] 从而指定spi发现的moduleProvider的特定实例作为实现
                    // 执行loadedProvider的prepare
                    newInstance.prepare(this, applicationConfiguration.getModuleConfiguration(moduleName), moduleProviderLoader);
                    loadedModules.put(moduleName, newInstance);
                    moduleList.remove(moduleName);
                }
            }
        }
        // Finish prepare stage
        isInPrepareStage = false;
        // 验在配置中的 Module 实现类的实例都创建
        if (moduleList.size() > 0) {
            throw new ModuleNotFoundException(moduleList.toString() + " missing.");
        }
        // 启动引导程序
        BootstrapFlow bootstrapFlow = new BootstrapFlow(loadedModules);
        // 执行 Module 启动逻辑
        bootstrapFlow.start(this);
        // 通知 ModuleProvider
        bootstrapFlow.notifyAfterCompleted();
    }

    @Override public boolean has(String moduleName) {
        return loadedModules.get(moduleName) != null;
    }

    @Override public ModuleProviderHolder find(String moduleName) throws ModuleNotFoundRuntimeException {
        assertPreparedStage();
        ModuleDefine module = loadedModules.get(moduleName);
        if (module != null)
            return module;
        throw new ModuleNotFoundRuntimeException(moduleName + " missing.");
    }

    private void assertPreparedStage() {
        if (isInPrepareStage) {
            throw new AssertionError("Still in preparing stage.");
        }
    }
}
