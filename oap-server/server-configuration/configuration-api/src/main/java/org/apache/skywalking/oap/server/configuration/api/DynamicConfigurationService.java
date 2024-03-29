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
 * DynamicConfigurationService provides API to register config change watcher.
 *
 * 根据底层依赖的配置中心不同 DynamicConfigurationService 包含不同的实现类
 * 比如Zookeeper,Nacos等 默认NoneConfigurationProvider 内的匿名内部类
 * @author wusheng
 */
public interface DynamicConfigurationService extends Service {
    /**
     * Register a watcher to the target value
     * 注册ConfigChangeWatcher监听器
     *
     *
     * @param watcher to register
     */
    void registerConfigChangeWatcher(ConfigChangeWatcher watcher);
}
