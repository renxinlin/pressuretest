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

package org.apache.skywalking.oap.server.telemetry.so11y;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.apache.skywalking.oap.server.library.module.*;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;
import org.apache.skywalking.oap.server.telemetry.api.MetricsCollector;
import org.apache.skywalking.oap.server.telemetry.api.MetricsCreator;

import java.io.IOException;

/**
 * Self observability telemetry provider.
 *
 * @author gaohongtao
 */
public class So11yTelemetryProvider extends ModuleProvider {
    private So11yConfig config;

    public So11yTelemetryProvider() {
        config = new So11yConfig();
    }

    @Override public String name() {
        return "so11y";
    }

    @Override public Class<? extends ModuleDefine> module() {
        return TelemetryModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return config;
    }

    @Override public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        this.registerServiceImplementation(MetricsCreator.class, new So11yMetricsCreator());
        this.registerServiceImplementation(MetricsCollector.class, new So11yMetricsCollector());
        if (config.isPrometheusExporterEnabled()) {
            try {
                new HTTPServer(config.getPrometheusExporterHost(), config.getPrometheusExporterPort());
            } catch (IOException e) {
                throw new ModuleStartException(e.getMessage(), e);
            }
        }
        DefaultExports.initialize();
    }

    @Override public void start() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
    }

    @Override public String[] requiredModules() {
        return new String[0];
    }
}
