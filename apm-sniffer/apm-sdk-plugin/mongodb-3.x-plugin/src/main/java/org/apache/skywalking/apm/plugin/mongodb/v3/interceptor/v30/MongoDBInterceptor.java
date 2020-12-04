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


package org.apache.skywalking.apm.plugin.mongodb.v3.interceptor.v30;

import com.mongodb.MongoNamespace;
import com.mongodb.connection.Cluster;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.pt.FlagValue;
import org.apache.skywalking.apm.plugin.mongodb.v3.support.MongoRemotePeerHelper;
import org.apache.skywalking.apm.plugin.mongodb.v3.support.MongoSpanHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Intercept method of {@code com.mongodb.Mongo#execute(ReadOperation, ReadPreference)} or
 * {@code com.mongodb.Mongo#execute(WriteOperation)}. record the MongoDB host, operation name and the key of the
 * operation.
 * <p>
 * only supported: 3.0.x-3.5.x
 *
 * @author scolia
 */
@SuppressWarnings({"deprecation", "Duplicates"})
public class MongoDBInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final ILog logger = LogManager.getLogger(MongoDBInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        Cluster cluster = (Cluster) allArguments[0];
        String peers = MongoRemotePeerHelper.getRemotePeer(cluster);
        objInst.setSkyWalkingDynamicField(peers);
    }


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) {
        String executeMethod = allArguments[0].getClass().getSimpleName();
        String remotePeer = (String) objInst.getSkyWalkingDynamicField();
        if (logger.isDebugEnable()) {
            logger.debug("Mongo execute: [executeMethod: {}, remotePeer: {}]", executeMethod, remotePeer);
        }
        MongoSpanHelper.createExitSpan(executeMethod, remotePeer, allArguments[0]);


        // 压测流量处理
        if (FlagValue.isPt()) {
            Object allArgument = allArguments[0];
            Class<?> aClass = allArgument.getClass();
            Field[] fields = allArgument.getClass().getDeclaredFields();
            try {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getName().equals("databaseName")) {
                        // 确定存在才修改Accessible
                        fields[i].setAccessible(true);
                        String databaseName = (String) fields[i].get(allArgument);
                        // 请求级别数据 if属于防御性编程
                        if (!databaseName.contains(FlagValue.PT_ROUTE_PREFIX)) {
                            databaseName = FlagValue.PT_ROUTE_PREFIX + databaseName;
                        }
                        fields[i].set(allArgument, databaseName);
                    }
                    //  RenameCollectionOperation rename 唯一特殊的一个[originalNamespace,newNamespace] 一般在生产中不存在这种操作,直接这样操作可能是致命的
                    if (fields[i].getName().equals("namespace")
                            || fields[i].getName().equals("originalNamespace")
                            || fields[i].getName().equals("newNamespace")) {
                        MongoNamespace namespace = (MongoNamespace) fields[i].get(allArgument);
                        // 请求级别数据 if属于防御性编程
                        if (!namespace.getDatabaseName().contains(FlagValue.PT_ROUTE_PREFIX)) {
                            Class<? extends MongoNamespace> namespaceClass = namespace.getClass();
                            Field databaseNameField = namespaceClass.getDeclaredField("databaseName");
                            Field fullNameField = namespaceClass.getDeclaredField("fullName");
                            databaseNameField.setAccessible(true);
                            fullNameField.setAccessible(true);
                            databaseNameField.set(namespace, FlagValue.PT_ROUTE_PREFIX + namespace.getDatabaseName());
                            fullNameField.set(namespace, FlagValue.PT_ROUTE_PREFIX + namespace.getFullName());
                        }
                    }
                }
            } catch (NoSuchFieldException e) {
                logger.error("mongoDb 压测流量处理失败..",e);
            } catch (IllegalAccessException e) {
                logger.error("mongoDb 压测流量处理失败...",e);
            }

        }

        // 参数是请求级别的 所以不用处理非压测流量

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.errorOccurred();
        activeSpan.log(t);
    }
}
