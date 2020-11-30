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

package org.apache.skywalking.apm.plugin.vertx3;

import io.vertx.core.http.HttpClientRequest;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

/**
 * @author brandon.fergerson
 */
public class HttpClientRequestImplEndInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        String host;
        int port;
        if (allArguments[3] instanceof Integer) {
            host = (String) allArguments[2];
            port = (Integer) allArguments[3];
        } else {
            host = (String) allArguments[3];
            port = (Integer) allArguments[4];
        }
        objInst.setSkyWalkingDynamicField(host + ":" + port);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        HttpClientRequest request = (HttpClientRequest) objInst;
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(toPath(request.uri()), contextCarrier,
                (String) objInst.getSkyWalkingDynamicField());
        span.setComponent(ComponentsDefine.VERTX);
        SpanLayer.asHttp(span);
        Tags.HTTP.METHOD.set(span, request.method().toString());
        Tags.URL.set(span, request.uri());

        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            request.headers().add(next.getHeadKey(), next.getHeadValue());
        }
        objInst.setSkyWalkingDynamicField(new VertxContext(ContextManager.capture(), span.prepareForAsync()));
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                                Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }

    private static String toPath(String uri) {
        int index = uri.indexOf("?");
        if (index > -1) {
            return uri.substring(0, index);
        } else {
            return uri;
        }
    }
}
