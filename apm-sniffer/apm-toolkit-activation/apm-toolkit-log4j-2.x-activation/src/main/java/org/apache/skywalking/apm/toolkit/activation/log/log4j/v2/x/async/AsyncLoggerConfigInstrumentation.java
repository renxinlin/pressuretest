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


package org.apache.skywalking.apm.toolkit.activation.log.log4j.v2.x.async;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * Instrument to intercept AsyncLoggerConfig of log4j2, .
 * Finally I choose populateLazilyInitializedFields as the intercept point, which all versions of AsyncAppender will actually work with
 * What's more, I choose callAppenders as the intercept point before, while it never works after log4j2 2.11.1+
 *
 * @author xuhe
 */

public class AsyncLoggerConfigInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String INTERCEPT_CLASS = "org.apache.skywalking.apm.toolkit.activation.log.log4j.v2.x.async.AsyncLoggerConfigMethodInterceptor";
    private static final String ENHANCE_CLASS = "org.apache.logging.log4j.core.async.AsyncLoggerConfig";
    private static final String ENHANCE_METHOD = "populateLazilyInitializedFields";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_METHOD);
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPT_CLASS;
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
        return byName(ENHANCE_CLASS);
    }
}
