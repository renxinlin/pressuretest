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
        RunningMode.setMode(mode);

        ApplicationConfigLoader configLoader = new ApplicationConfigLoader();
        ModuleManager manager = new ModuleManager();
        try {
            // 加载配置
            ApplicationConfiguration applicationConfiguration = configLoader.load();
            // 初始化组件
            /**
             * spi
             * org.apache.skywalking.oap.server.library.module.ModuleDefine
             * org.apache.skywalking.oap.server.library.module.ModuleProvider
             */
            manager.init(applicationConfiguration);

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
