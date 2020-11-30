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


package org.apache.skywalking.apm.plugin.jedis.v2;

import java.lang.reflect.Method;

import org.apache.skywalking.apm.agent.core.context.AbstractTracerContext;
import org.apache.skywalking.apm.agent.core.context.TracingContext;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import redis.clients.jedis.Jedis;

public class JedisMethodInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan("Jedis/" + method.getName(), peer);
        span.setComponent(ComponentsDefine.JEDIS);
        Tags.DB_TYPE.set(span, "Redis");
        SpanLayer.asCache(span);

        if (allArguments.length > 0 && allArguments[0] instanceof String) {
            Tags.DB_STATEMENT.set(span, method.getName() + " " + allArguments[0]);
        } else if (allArguments.length > 0 && allArguments[0] instanceof byte[]) {
            Tags.DB_STATEMENT.set(span, method.getName());
        }
        // 这里判断 如果请求存在压测标志
        // 对于 定时任务这种 需要业务方手工处理 开一套影子处理
        TracingContext abstractTracerContext = (TracingContext) ContextManager.get();
        boolean pressureTest = abstractTracerContext.getSegment().isPressureTest();
        if(pressureTest) {
            // todo 需要根据不同的参数判断key处于数组的位置 从而进行处理 目前不处理 参见下个TODO
            Jedis objInstReal = (Jedis) objInst;
            // 压测数据源
            long addTwoDB = (objInstReal.getDB() + 1) % 16;
            objInstReal.select((int)addTwoDB);
            // 数据也加上偏移 todo 实际使用还是要加上偏移 但是由于redis的操作太多 需要分类处理 这里占时不做分类实现 直接切库
            // allArguments[0] = "shadow:" + allArguments[0];
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }
}
