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

package org.apache.skywalking.apm.plugin.jdk.http.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;

/**
 * @author lican
 */
public class HttpsClientInstrumentation extends ClassEnhancePluginDefine {

    private static final String ENHANCE_HTTPS_CLASS = "sun.net.www.protocol.https.HttpsClient";

    private static final String NEW_INSTANCE_METHOD = "New";

    private static final String INTERCEPT_HTTPS_NEW_INSTANCE_CLASS = "org.apache.skywalking.apm.plugin.jdk.http.HttpsClientNewInstanceInterceptor";


    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[0];
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[]{new StaticMethodsInterceptPoint() {
            @Override
            public ElementMatcher<MethodDescription> getMethodsMatcher() {
                return named(NEW_INSTANCE_METHOD)
                        .and(takesArguments(7)
                                .and(takesArgumentWithType(0, "javax.net.ssl.SSLSocketFactory"))
                                .and(takesArgumentWithType(3, "java.net.Proxy"))
                                .and(takesArgumentWithType(6, "sun.net.www.protocol.http.HttpURLConnection"))
                        );
            }

            @Override
            public String getMethodsInterceptor() {
                return INTERCEPT_HTTPS_NEW_INSTANCE_CLASS;
            }

            @Override
            public boolean isOverrideArgs() {
                return false;
            }
        }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_HTTPS_CLASS);
    }


    @Override
    public boolean isBootstrapInstrumentation() {
        return true;
    }
}