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

package org.apache.skywalking.apm.plugin.rabbitmq.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;

public class RabbitMQProducerInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    public static final String INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.rabbitmq.RabbitMQProducerInterceptor";
    public static final String ENHANCE_CLASS_PRODUCER = "com.rabbitmq.client.impl.ChannelN";
    public static final String ENHANCE_METHOD_DISPATCH = "basicPublish";
    public static final String INTERCEPTOR_CONSTRUCTOR = "org.apache.skywalking.apm.plugin.rabbitmq.RabbitMQProducerAndConsumerConstructorInterceptor";
    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] {
            new ConstructorInterceptPoint() {
                    @Override public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(3,"com.rabbitmq.client.MetricsCollector");
                    }

                    @Override public String getConstructorInterceptor() {
                        return INTERCEPTOR_CONSTRUCTOR;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                    @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(ENHANCE_METHOD_DISPATCH).and(takesArgumentWithType(4,"com.rabbitmq.client.AMQP$BasicProperties"));
                    }


                    @Override public String getMethodsInterceptor() {
                        return INTERCEPTOR_CLASS;
                    }

                    @Override public boolean isOverrideArgs() {
                        return true;
                    }
                }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return MultiClassNameMatch.byMultiClassMatch(ENHANCE_CLASS_PRODUCER);
    }
}
