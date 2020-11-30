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

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.trace.*;
import org.junit.*;

public class TracingContextTest {
    @BeforeClass
    public static void setup() {
        ServiceManager.INSTANCE.boot();
        RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID = 5;
    }

    @AfterClass
    public static void clear() {
        RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID = 0;
        ServiceManager.INSTANCE.shutdown();
    }

    @Test
    public void testSpanLimit() {
        final boolean[] dataReceived = {false};
        TracingContextListener listener = new TracingContextListener() {
            @Override public void afterFinished(TraceSegment traceSegment) {
                dataReceived[0] = true;
            }
        };
        TracingContext.ListenerManager.add(listener);
        try {
            TracingContext tracingContext = new TracingContext();
            AbstractSpan span = tracingContext.createEntrySpan("/url");

            for (int i = 0; i < 10; i++) {
                AbstractSpan localSpan = tracingContext.createLocalSpan("/java-bean");

                for (int j = 0; j < 100; j++) {
                    AbstractSpan exitSpan = tracingContext.createExitSpan("/redis", "localhost");
                    tracingContext.stopSpan(exitSpan);
                }

                tracingContext.stopSpan(localSpan);
            }

            tracingContext.stopSpan(span);

            Assert.assertTrue(dataReceived[0]);
        } finally {
            TracingContext.ListenerManager.remove(listener);
        }
    }


}
