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

package org.apache.skywalking.apm.plugin.pulsar;

import org.apache.pulsar.common.api.proto.PulsarApi;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SegmentRefHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.SpanAssert;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.List;

import static org.apache.skywalking.apm.network.trace.component.ComponentsDefine.PULSAR_CONSUMER;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class PulsarConsumerInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private ConsumerEnhanceRequiredInfo consumerEnhanceRequiredInfo;

    private PulsarConsumerInterceptor consumerInterceptor;

    private MockMessage msg;

    private EnhancedInstance consumerInstance = new EnhancedInstance() {
        @Override public Object getSkyWalkingDynamicField() {
            return consumerEnhanceRequiredInfo;
        }

        @Override public void setSkyWalkingDynamicField(Object value) {
            consumerEnhanceRequiredInfo = (ConsumerEnhanceRequiredInfo)value;
        }
    };

    @Before
    public void setUp() {
        Config.Agent.ACTIVE_V1_HEADER = true;
        consumerInterceptor = new PulsarConsumerInterceptor();
        consumerEnhanceRequiredInfo = new ConsumerEnhanceRequiredInfo();

        consumerEnhanceRequiredInfo.setTopic("persistent://my-tenant/my-ns/my-topic");
        consumerEnhanceRequiredInfo.setServiceUrl("pulsar://localhost:6650");
        consumerEnhanceRequiredInfo.setSubscriptionName("my-sub");
        msg = new MockMessage();
        msg.getMessageBuilder().addProperties(PulsarApi.KeyValue.newBuilder()
                .setKey("sw3")
                .setValue("1.234.111|3|1|1|#192.168.1.8:18002|#/portal/|#testEntrySpan|#AQA*#AQA*Et0We0tQNQA*"));
    }

    @After
    public void clear() {
        Config.Agent.ACTIVE_V1_HEADER = false;
    }

    @Test
    public void testConsumerWithNullMessage() throws Throwable {
        consumerInterceptor.beforeMethod(consumerInstance, null, new Object[]{null}, new Class[0], null);
        consumerInterceptor.afterMethod(consumerInstance, null, new Object[]{null}, new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(0));
    }

    @Test
    public void testConsumerWithMessage() throws Throwable {
        consumerInterceptor.beforeMethod(consumerInstance, null, new Object[]{msg}, new Class[0], null);
        consumerInterceptor.afterMethod(consumerInstance, null, new Object[]{msg}, new Class[0], null);

        List<TraceSegment> traceSegments = segmentStorage.getTraceSegments();
        assertThat(traceSegments.size(), is(1));

        TraceSegment traceSegment = traceSegments.get(0);
        List<TraceSegmentRef> refs = traceSegment.getRefs();
        assertThat(refs.size(), is(1));
        assertTraceSegmentRef(refs.get(0));

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertConsumerSpan(spans.get(0));
    }

    private void assertConsumerSpan(AbstractTracingSpan span) {
        SpanAssert.assertLayer(span, SpanLayer.MQ);
        SpanAssert.assertComponent(span, PULSAR_CONSUMER);
        SpanAssert.assertTagSize(span, 2);
        SpanAssert.assertTag(span, 0, "pulsar://localhost:6650");
        SpanAssert.assertTag(span, 1, "persistent://my-tenant/my-ns/my-topic");
    }

    private void assertTraceSegmentRef(TraceSegmentRef ref) {
        MatcherAssert.assertThat(SegmentRefHelper.getEntryServiceInstanceId(ref), is(1));
        MatcherAssert.assertThat(SegmentRefHelper.getSpanId(ref), is(3));
        MatcherAssert.assertThat(SegmentRefHelper.getTraceSegmentId(ref).toString(), is("1.234.111"));
    }
}
