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

package org.apache.skywalking.apm.plugin.servicecomb.v1;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.servicecomb.core.Endpoint;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.core.definition.OperationMeta;
import org.apache.servicecomb.core.definition.SchemaMeta;
import org.apache.servicecomb.core.provider.consumer.ReferenceConfig;
import org.apache.servicecomb.swagger.invocation.InvocationType;
import org.apache.servicecomb.swagger.invocation.SwaggerInvocation;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class TransportClientHandlerInterceptorTest {

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();
    @Mock
    Response.StatusType statusType;
    @Mock
    ReferenceConfig referenceConfig;
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;
    private TransportClientHandlerInterceptor nextInterceptor;
    @Mock
    private OperationMeta operationMeta;
    @Mock
    private EnhancedInstance enhancedInstance;
    @Mock
    private Invocation invocation;
    @Mock
    private Endpoint endpoint;
    @Mock
    private SwaggerInvocation swagger;
    private Object[] allArguments;
    private Class[] argumentsType;
    private Object[] swaggerArguments;

    @Mock
    private SchemaMeta schemaMeta;

    @Before
    public void setUp() throws Exception {
        ServiceManager.INSTANCE.boot();
        nextInterceptor = new TransportClientHandlerInterceptor();
        PowerMockito.mock(Invocation.class);
        when(operationMeta.getSchemaMeta()).thenReturn(schemaMeta);
        when(endpoint.getAddress()).thenReturn("0.0.0.0:7777");
        when(invocation.getEndpoint()).thenReturn(endpoint);
        when(invocation.getMicroserviceQualifiedName()).thenReturn("consumerTest");
        when(operationMeta.getOperationPath()).thenReturn("/bmi");
        when(invocation.getOperationMeta()).thenReturn(operationMeta);
        when(invocation.getStatus()).thenReturn(statusType);
        when(statusType.getStatusCode()).thenReturn(200);
        when(invocation.getInvocationType()).thenReturn(InvocationType.CONSUMER);
        Config.Agent.SERVICE_NAME = "serviceComnTestCases-APP";

        allArguments = new Object[] {invocation,};
        argumentsType = new Class[] {};
        swaggerArguments = new Class[] {};
    }

    @Test
    public void testConsumer() throws Throwable {
        nextInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        nextInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, null);
        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertCombSpan(spans.get(0));
        verify(invocation).getContext();
    }

    private void assertCombSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("consumerTest"));
        assertThat(SpanHelper.getComponentId(span), is(28));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("/bmi"));
        assertThat(span.isExit(), is(true));
    }

}
