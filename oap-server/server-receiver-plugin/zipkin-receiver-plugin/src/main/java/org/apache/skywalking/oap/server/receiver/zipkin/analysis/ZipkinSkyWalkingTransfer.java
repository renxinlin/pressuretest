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

package org.apache.skywalking.oap.server.receiver.zipkin.analysis;

import java.util.List;
import org.apache.skywalking.oap.server.receiver.sharing.server.CoreRegisterLinker;
import org.apache.skywalking.oap.server.receiver.zipkin.*;
import org.apache.skywalking.oap.server.receiver.zipkin.analysis.cache.CacheFactory;
import zipkin2.Span;

public class ZipkinSkyWalkingTransfer {
    public void doTransfer(ZipkinReceiverConfig config, List<Span> spanList) {
        spanList.forEach(span -> {
            // In Zipkin, the local service name represents the application owner.
            String applicationCode = span.localServiceName();
            if (applicationCode != null) {
                int applicationId = CoreRegisterLinker.getServiceInventoryRegister().getOrCreate(applicationCode, null);
                if (applicationId != 0) {
                    CoreRegisterLinker.getServiceInstanceInventoryRegister().getOrCreate(applicationId, applicationCode, applicationCode,
                        span.timestampAsLong(),
                        ZipkinTraceOSInfoBuilder.getOSInfoForZipkin(applicationCode));
                }
            }

            CacheFactory.INSTANCE.get(config).addSpan(span);
        });
    }
}
