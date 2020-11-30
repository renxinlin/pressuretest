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


package org.apache.skywalking.apm.plugin.mongodb.v3.define.v38;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

/**
 * same whit {@link org.apache.skywalking.apm.plugin.mongodb.v3.define.v37.MongoDBOperationExecutorInstrumentation}
 * <p>
 * support: 3.8.x or higher
 *
 * @author scolia
 */
public class MongoDBOperationExecutorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String WITNESS_CLASS = "com.mongodb.client.ClientSession";

    private static final String ENHANCE_CLASS = "com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor";

    private static final String INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.mongodb.v3.interceptor.v37.MongoDBOperationExecutorInterceptor";

    private static final String METHOD_NAME = "execute";

    private static final String ARGUMENT_TYPE = "com.mongodb.client.ClientSession";

    @Override
    protected String[] witnessClasses() {
        return new String[]{WITNESS_CLASS};
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{new InstanceMethodsInterceptPoint() {
            @Override
            public ElementMatcher<MethodDescription> getMethodsMatcher() {
                return ElementMatchers
                        // 3.8.x~3.11.x
                        .named(METHOD_NAME).and(ArgumentTypeNameMatch.takesArgumentWithType(2, ARGUMENT_TYPE))
                        .or(ElementMatchers.<MethodDescription>named(METHOD_NAME)
                                .and(ArgumentTypeNameMatch.takesArgumentWithType(3, ARGUMENT_TYPE))
                        );
            }

            @Override
            public String getMethodsInterceptor() {
                return INTERCEPTOR_CLASS;
            }

            @Override
            public boolean isOverrideArgs() {
                return false;
            }
        }
        };
    }
}
