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

package org.apache.skywalking.oap.server.receiver.jvm.provider.handler;

import io.grpc.stub.StreamObserver;
import org.apache.skywalking.apm.network.common.Commands;
import org.apache.skywalking.apm.network.language.agent.v2.*;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.slf4j.*;

public class JVMMetricReportServiceHandler extends JVMMetricReportServiceGrpc.JVMMetricReportServiceImplBase implements GRPCHandler {

    private static final Logger logger = LoggerFactory.getLogger(JVMMetricReportServiceHandler.class);

    private final JVMSourceDispatcher jvmSourceDispatcher;

    public JVMMetricReportServiceHandler(ModuleManager moduleManager) {
        this.jvmSourceDispatcher = new JVMSourceDispatcher(moduleManager);
    }

    @Override public void collect(JVMMetricCollection request, StreamObserver<Commands> responseObserver) {
        int serviceInstanceId = request.getServiceInstanceId();

        if (logger.isDebugEnabled()) {
            logger.debug("receive the jvm metrics from service instance, id: {}", serviceInstanceId);
        }

        request.getMetricsList().forEach(metrics -> {
            long minuteTimeBucket = TimeBucket.getMinuteTimeBucket(metrics.getTime());
            jvmSourceDispatcher.sendMetric(serviceInstanceId, minuteTimeBucket, metrics);
        });

        responseObserver.onNext(Commands.newBuilder().build());
        responseObserver.onCompleted();
    }

}
