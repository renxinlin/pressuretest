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

package org.apache.skywalking.oap.server.starter;

import org.apache.skywalking.oap.server.core.RunningMode;
import org.apache.skywalking.oap.server.library.module.ApplicationConfiguration;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.starter.config.ApplicationConfigLoader;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;
import org.apache.skywalking.oap.server.telemetry.api.MetricsCreator;
import org.apache.skywalking.oap.server.telemetry.api.MetricsTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 * @author kezhenxu94
 */
public class OAPServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(OAPServerBootstrap.class);

    public static void start() {
        String mode = System.getProperty("mode");
        // init 或者 no-init 表示是否初始化[example: 底层存储组件等]
        RunningMode.setMode(mode);

        /*
        初始化ApplicationConfiguration 通过ApplicationConfiguration加载ApplicationConfiguration
         */
        ApplicationConfigLoader configLoader = new ApplicationConfigLoader();
        ModuleManager manager = new ModuleManager();
        try {

            /*
                                一个ApplicationConfiguration有多个ModuleConfiguration
                                一个ModuleConfiguration有多个ProviderConfiguration

                                Module对应模块,比如存储模块  集群模块,服务网格模块等等
                                Provider对应模块的实现集合,模块最终会选择且仅选择一个提供者作为模块实现


                                                                    ApplicationConfiguration

                 ModuleConfiguration                                   ModuleConfiguration                               ModuleConfiguration
                      /\                                                     /\                                                    /\
 ProviderConfiguration  ProviderConfiguration           ProviderConfiguration  ProviderConfiguration          ProviderConfiguration  ProviderConfiguration

             */
            // 加载yml生成ApplicationConfiguration配置
            ApplicationConfiguration applicationConfiguration = configLoader.load();
            // 初始化模块
            /**
             * spi
             * org.apache.skywalking.oap.server.library.module.ModuleDefine
             * org.apache.skywalking.oap.server.library.module.ModuleProvider
             */
            manager.init(applicationConfiguration);
            // 根据模块名获取模块,根据模块或者loadedProvider,根据Provider获取内部service集合中的MetricsCreator
            // 模块有多个provider,有且仅有一个作为loadedProvider工作
            // Provider由多个service提供工作
            manager.find(TelemetryModule.NAME).provider().getService(MetricsCreator.class).createGauge("uptime",
                "oap server start up time", MetricsTag.EMPTY_KEY, MetricsTag.EMPTY_VALUE)
                // Set uptime to second
                .setValue(System.currentTimeMillis() / 1000d);

            if (RunningMode.isInitMode()) {
                logger.info("OAP starts up in init mode successfully, exit now...");
                System.exit(0);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            System.exit(1);
        }
    }
}
