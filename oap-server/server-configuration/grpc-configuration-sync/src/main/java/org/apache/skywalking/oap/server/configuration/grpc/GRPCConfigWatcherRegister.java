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

package org.apache.skywalking.oap.server.configuration.grpc;

import io.grpc.netty.NettyChannelBuilder;
import org.apache.skywalking.oap.server.configuration.api.*;
import org.apache.skywalking.oap.server.configuration.service.*;
import org.slf4j.*;

import java.util.Set;

/**
 * @author wusheng
 */
public class GRPCConfigWatcherRegister extends ConfigWatcherRegister {
    private static final Logger logger = LoggerFactory.getLogger(GRPCConfigWatcherRegister.class);

    private RemoteEndpointSettings settings;
    private ConfigurationServiceGrpc.ConfigurationServiceBlockingStub stub;

    public GRPCConfigWatcherRegister(RemoteEndpointSettings settings) {
        super(settings.getPeriod());
        this.settings = settings;
        stub = ConfigurationServiceGrpc.newBlockingStub(NettyChannelBuilder.forAddress(settings.getHost(), settings.getPort()).usePlaintext().build());
    }

    @Override public ConfigTable readConfig(Set<String> keys) {
        ConfigTable table = new ConfigTable();
        try {
            ConfigurationResponse response = stub.call(ConfigurationRequest.newBuilder().setClusterName(settings.getClusterName()).build());
            response.getConfigTableList().forEach(config -> {
                final String name = config.getName();
                if (keys.contains(name)) {
                    table.add(new ConfigTable.ConfigItem(name, config.getValue()));
                }
            });
        } catch (Exception e) {
            logger.error("Remote config center [" + settings + "] is not available.", e);
        }
        return table;
    }
}
