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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wu-sheng, peng-yongsheng
 */
class BootstrapFlow {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapFlow.class);

    private Map<String, ModuleDefine> loadedModules;
    private List<ModuleProvider> startupSequence;

    BootstrapFlow(Map<String, ModuleDefine> loadedModules) throws CycleDependencyException {
        this.loadedModules = loadedModules;
        startupSequence = new LinkedList<>();
        // 初始化startupSequence 中的所有ModuleProvider
        makeSequence();
    }

    @SuppressWarnings("unchecked")
    void start(
        ModuleManager moduleManager) throws ModuleNotFoundException, ServiceNotProvidedException, ModuleStartException {
        for (ModuleProvider provider : startupSequence) {
            // 当前ModuleProvider对应的Module依赖的其他module
            // 比如存储模块StorageModule的其中一个实现StorageModuleElasticsearch7Provider依赖核心模块CoreModule.NAME
            String[] requiredModules = provider.requiredModules();
            if (requiredModules != null) {
                for (String module : requiredModules) {
                    if (!moduleManager.has(module)) {
                        throw new ModuleNotFoundException(module + " is required by " + provider.getModuleName()
                            + "." + provider.name() + ", but not found.");
                    }
                }
            }
            logger.info("start the provider {} in {} module.", provider.name(), provider.getModuleName());
            // 检查所有需要的service是不是存在 Module不同的provider有不同的services实现
            // module负责定义功能,services方法指定了一系列service接口
            // 则module的实现provider需要实现该系列的service 通过requiredCheck检查service是否全部实现

            // 一般我们在provider.prepare时通过registerServiceImplementation注册serviceImpl
            provider.requiredCheck(provider.getModule().services());
            // 启动provider 比如es7存储实现则创建esClient 并且根据init模式决定是否在es上初始化索引
            provider.start();
        }
    }

    void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
        for (ModuleProvider provider : startupSequence) {
            provider.notifyAfterCompleted();
        }
    }

    private void makeSequence() throws CycleDependencyException {
        List<ModuleProvider> allProviders = new ArrayList<>();
        loadedModules.forEach((moduleName, module) -> allProviders.add(module.provider()));

        do {
            int numOfToBeSequenced = allProviders.size();
            for (int i = 0; i < allProviders.size(); i++) {
                ModuleProvider provider = allProviders.get(i);
                String[] requiredModules = provider.requiredModules();
                if (CollectionUtils.isNotEmpty(requiredModules)) {
                    boolean isAllRequiredModuleStarted = true;
                    for (String module : requiredModules) {
                        // find module in all ready existed startupSequence
                        boolean exist = false;
                        for (ModuleProvider moduleProvider : startupSequence) {
                            if (moduleProvider.getModuleName().equals(module)) {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist) {
                            isAllRequiredModuleStarted = false;
                            break;
                        }
                    }

                    if (isAllRequiredModuleStarted) {
                        startupSequence.add(provider);
                        allProviders.remove(i);
                        i--;
                    }
                } else {
                    startupSequence.add(provider);
                    allProviders.remove(i);
                    i--;
                }
            }

            if (numOfToBeSequenced == allProviders.size()) {
                StringBuilder unSequencedProviders = new StringBuilder();
                allProviders.forEach(provider -> unSequencedProviders.append(provider.getModuleName()).append("[provider=").append(provider.getClass().getName()).append("]\n"));
                throw new CycleDependencyException("Exist cycle module dependencies in \n" + unSequencedProviders.substring(0, unSequencedProviders.length() - 1));
            }
        }
        while (allProviders.size() != 0);
    }
}
