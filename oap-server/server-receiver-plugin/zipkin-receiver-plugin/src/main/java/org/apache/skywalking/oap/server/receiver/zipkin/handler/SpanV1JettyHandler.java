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

package org.apache.skywalking.oap.server.receiver.zipkin.handler;

import javax.servlet.http.*;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.cache.*;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.jetty.JettyHandler;
import org.apache.skywalking.oap.server.receiver.zipkin.ZipkinReceiverConfig;
import org.slf4j.*;
import zipkin2.codec.SpanBytesDecoder;

public class SpanV1JettyHandler extends JettyHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpanV1JettyHandler.class);

    private ZipkinReceiverConfig config;
    private SourceReceiver sourceReceiver;
    private ServiceInventoryCache serviceInventoryCache;
    private EndpointInventoryCache endpointInventoryCache;

    public SpanV1JettyHandler(ZipkinReceiverConfig config,
        ModuleManager manager) {
        sourceReceiver = manager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        serviceInventoryCache = manager.find(CoreModule.NAME).provider().getService(ServiceInventoryCache.class);
        endpointInventoryCache = manager.find(CoreModule.NAME).provider().getService(EndpointInventoryCache.class);
        this.config = config;
    }

    @Override
    public String pathSpec() {
        return "/api/v1/spans";
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        try {
            String type = request.getHeader("Content-Type");

            int encode = type != null && type.contains("/x-thrift") ? SpanEncode.THRIFT : SpanEncode.JSON_V1;

            SpanBytesDecoder decoder = SpanEncode.isThrift(encode)
                ? SpanBytesDecoder.THRIFT
                : SpanBytesDecoder.JSON_V1;

            SpanProcessor processor = new SpanProcessor(sourceReceiver, serviceInventoryCache, endpointInventoryCache, encode);
            processor.convert(config, decoder, request);

            response.setStatus(202);
        } catch (Exception e) {
            response.setStatus(500);

            logger.error(e.getMessage(), e);
        }
    }

}
