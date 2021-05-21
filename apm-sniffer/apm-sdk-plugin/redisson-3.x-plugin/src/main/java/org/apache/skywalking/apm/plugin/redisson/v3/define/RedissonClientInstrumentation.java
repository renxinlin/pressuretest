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
package org.apache.skywalking.apm.plugin.redisson.v3.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * @author zhaoyuguang
 */
public class RedissonClientInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "org.redisson.Redisson";

    private static final String REDIS_CLIENT_INTERCEPTOR = "org.apache.skywalking.apm.plugin.redisson.v3.RedissonClientConstructorInterceptor";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }


    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[0];
//        {
//            new InstanceMethodsInterceptPoint() {
//
//                @Override
//                public ElementMatcher<MethodDescription> getMethodsMatcher() {
//                    // todo 所有获取对象的操作全部加上压测拦截
//                    return null;
//                }
//
//                @Override
//                public String getMethodsInterceptor() {
//                    return REDIS_CLIENT_INTERCEPTOR;
//                }
//
//                @Override
//                public boolean isOverrideArgs() {
//                    return true;
//                }
//            }
//        };
    }

    @Override
    public ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
