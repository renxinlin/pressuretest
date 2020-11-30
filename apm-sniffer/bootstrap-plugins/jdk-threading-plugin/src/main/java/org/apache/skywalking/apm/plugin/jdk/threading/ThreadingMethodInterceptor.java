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

package org.apache.skywalking.apm.plugin.jdk.threading;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author kezhenxu94
 */
public class ThreadingMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(
        final EnhancedInstance objInst,
        final Method method,
        final Object[] allArguments,
        final Class<?>[] argumentsTypes,
        final MethodInterceptResult result) {
        // 这里之前说过 创建span 会创建线程级别的trace上下文 而trace上下文 又包含tracesegment
        AbstractSpan span = ContextManager.createLocalSpan(generateOperationName(objInst, method));
        span.setComponent(ComponentsDefine.JDK_THREADING);

        final Object storedField = objInst.getSkyWalkingDynamicField();
        if (storedField != null) {
            final ContextSnapshot contextSnapshot = (ContextSnapshot) storedField;
            // 这里就是设置当前上下文tracesegment与前一个tracesegment的关系 以及主链路id
            ContextManager.continued(contextSnapshot);
        }

    }

    @Override
    public Object afterMethod(
        final EnhancedInstance objInst,
        final Method method,
        final Object[] allArguments,
        final Class<?>[] argumentsTypes,
        final Object ret) {

        final Object storedField = objInst.getSkyWalkingDynamicField();
        if (storedField != null) {
            ContextManager.stopSpan();
        }

        return ret;
    }

    @Override
    public void handleMethodException(
        final EnhancedInstance objInst,
        final Method method,
        final Object[] allArguments,
        final Class<?>[] argumentsTypes,
        final Throwable t) {

        if (ContextManager.isActive()) {
            ContextManager.activeSpan().errorOccurred().log(t);
        }
    }

    private String generateOperationName(final EnhancedInstance objInst, final Method method) {
        return "Threading/" + objInst.getClass().getName() + "/" + method.getName();
    }

}
