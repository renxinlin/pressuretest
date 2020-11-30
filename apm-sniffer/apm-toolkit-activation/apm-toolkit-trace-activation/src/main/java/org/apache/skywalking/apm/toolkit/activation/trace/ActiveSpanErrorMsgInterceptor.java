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

package org.apache.skywalking.apm.toolkit.activation.trace;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author caoyixiong
 */
public class ActiveSpanErrorMsgInterceptor implements StaticMethodsAroundInterceptor {
    @Override public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                                       MethodInterceptResult result) {
        AbstractSpan activeSpan = null;
        try {
            activeSpan = ContextManager.activeSpan();
            activeSpan.errorOccurred();
            Map<String, String> event = new HashMap<String, String>();
            event.put("event", "error");
            event.put("message", String.valueOf(allArguments[0]));
            activeSpan.log(System.currentTimeMillis(), event);

        } catch (NullPointerException e) {
        }
    }

    @Override public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                                        Object ret) {
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
                                      Throwable t) {

    }
}