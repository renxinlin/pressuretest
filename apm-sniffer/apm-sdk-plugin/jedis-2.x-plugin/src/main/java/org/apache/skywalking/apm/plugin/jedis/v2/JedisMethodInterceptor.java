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
import org.apache.skywalking.apm.agent.core.pt.StaticRoutingInfo;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import redis.clients.jedis.Jedis;

/**
 * 压测不支持jedis集群
 * jedis只能使用一个库
 * 不支持在业务中中调用select
 *
 * 后期全部改造成基于: 前缀key
 */
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
        Jedis objInstReal = (Jedis) objInst;
        if(pressureTest) {
            // 压测数据源
            objInstReal.select(StaticRoutingInfo.ptRedisDb.intValue());
        }else {
            // 真实数据源 可能不需要
            objInstReal.select(StaticRoutingInfo.redisDb.intValue());

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
