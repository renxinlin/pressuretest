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

package org.apache.skywalking.oap.server.configuration.zookeeper;

import com.google.common.base.Strings;
import org.apache.skywalking.oap.server.configuration.api.AbstractConfigurationProvider;
import org.apache.skywalking.oap.server.configuration.api.ConfigWatcherRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;

/**
 * Get configuration from Zookeeper.
 *
 * @author zhaoyuguang
 */
public class ZookeeperConfigurationProvider extends AbstractConfigurationProvider {
    private ZookeeperServerSettings settings;

    public ZookeeperConfigurationProvider() {
        settings = new ZookeeperServerSettings();
    }

    @Override
    public String name() {
        return "zookeeper";
    }

    //  yml 对应的Provider中取得相关配置
    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return settings;
    }

    @Override
    protected ConfigWatcherRegister initConfigReader() throws ModuleStartException {
        if (Strings.isNullOrEmpty(settings.getHostPort())) {
            throw new ModuleStartException("Zookeeper hostPort cannot be null or empty.");
        }
        if (Strings.isNullOrEmpty(settings.getNameSpace())) {
            throw new ModuleStartException("Zookeeper nameSpace cannot be null or empty.");
        }

        try {
            // zk的配置信息
            return new ZookeeperConfigWatcherRegister(settings);
        } catch (Exception e) {
            throw new ModuleStartException(e.getMessage(), e);
        }
    }
}
