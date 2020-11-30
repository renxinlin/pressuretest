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

package org.apache.skywalking.oap.server.configuration.nacos;

import org.apache.skywalking.oap.server.configuration.api.ConfigChangeWatcher;
import org.apache.skywalking.oap.server.configuration.api.ConfigurationModule;
import org.apache.skywalking.oap.server.configuration.api.DynamicConfigurationService;
import org.apache.skywalking.oap.server.library.module.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kezhenxu94
 */
public class NacosConfigurationTestProvider extends ModuleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfigurationTestProvider.class);

    ConfigChangeWatcher watcher;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return NacosConfigurationTestModule.class;
    }

    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return new ModuleConfig() {
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        watcher = new ConfigChangeWatcher(NacosConfigurationTestModule.NAME, this, "testKey") {
            private volatile String testValue;

            @Override
            public void notify(ConfigChangeWatcher.ConfigChangeEvent value) {
                LOGGER.info("ConfigChangeWatcher.ConfigChangeEvent: {}", value);
                if (EventType.DELETE.equals(value.getEventType())) {
                    testValue = null;
                } else {
                    testValue = value.getNewValue();
                }
            }

            @Override
            public String value() {
                return testValue;
            }
        };
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        getManager().find(ConfigurationModule.NAME)
            .provider()
            .getService(DynamicConfigurationService.class)
            .registerConfigChangeWatcher(watcher);
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public String[] requiredModules() {
        return new String[] {
            ConfigurationModule.NAME
        };
    }
}
