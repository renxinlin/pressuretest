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

package org.apache.skywalking.oap.server.configuration.api;

import org.apache.skywalking.oap.server.library.module.*;

/**
 * A nutshell configuration implementor.
 *
 * @author wusheng
 */
public class NoneConfigurationProvider extends ModuleProvider {
    @Override public String name() {
        return "none";
    }

    @Override public Class<? extends ModuleDefine> module() {
        return ConfigurationModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return null;
    }

    @Override public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        this.registerServiceImplementation(DynamicConfigurationService.class, new DynamicConfigurationService() {
            // 也没有定时扫描
            @Override public void registerConfigChangeWatcher(ConfigChangeWatcher watcher) {
                // 配置module的NoneConfigurationProvider 无需监听数据的变更

            }
        });
    }

    @Override public void start() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override public String[] requiredModules() {
        return new String[0];
    }
}
