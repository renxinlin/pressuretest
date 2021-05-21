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
package org.apache.skywalking.apm.plugin.redisson.v3;

import org.apache.skywalking.apm.agent.core.context.RuntimeContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.pt.FlagValue;
import org.redisson.Redisson;
import org.redisson.config.*;

import java.lang.reflect.Method;

/**
 * redisson 增加压测流量路由
 */
public class RedissonClientConstructorInterceptor implements InstanceMethodsAroundInterceptor {


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        boolean pt = FlagValue.isPt();
        // 压测数据偏移
//        int oldDb = 0;
        if (pt) {
            Redisson redisClient = (Redisson) objInst;
            Config config = redisClient.getConfig();
            // 单机模式
            try {
                SingleServerConfig singleServerConfig = config.useSingleServer();
//                oldDb = singleServerConfig.getDatabase();
                int dbOffset = (singleServerConfig.getDatabase() + 1) % 16;
                singleServerConfig.setDatabase(dbOffset);
//                ContextManager.getRuntimeContext().put("redisType",'1');
            } catch (Exception e) {

            }

            // 集群 todo
            try {
                ClusterServersConfig clusterServersConfig = config.useClusterServers();
//                ContextManager.getRuntimeContext().put("redisType",'2');
            } catch (Exception e) {

            }

            // 主从
            try {
                MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers();
//                oldDb = masterSlaveServersConfig.getDatabase();
                int dbOffset = (masterSlaveServersConfig.getDatabase() + 1) % 16;
                masterSlaveServersConfig.setDatabase(dbOffset);
//                ContextManager.getRuntimeContext().put("redisType",'3');
            } catch (Exception e) {

            }


            // 哨兵
            try {
                SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
//                oldDb = sentinelServersConfig.getDatabase();

                int dbOffset = (sentinelServersConfig.getDatabase() + 1) % 16;
                sentinelServersConfig.setDatabase(dbOffset);
//                ContextManager.getRuntimeContext().put("redisType",'4');

            } catch (Exception e) {

            }

            // 云托管模式【适用于任何由云计算运营商提供的Redis云服务，包括亚马逊云的AWS ElastiCache、微软云的Azure Redis 缓存和阿里云（Aliyun）的云数据库Redis版】
            try {
                ReplicatedServersConfig replicatedServersConfig = config.useReplicatedServers();
//                oldDb = replicatedServersConfig.getDatabase();
                int dbOffset = (replicatedServersConfig.getDatabase() + 1) % 16;
                replicatedServersConfig.setDatabase(dbOffset);
//                ContextManager.getRuntimeContext().put("redisType",'5');
            } catch (Exception e) {

            }

//            ContextManager.getRuntimeContext().put("olddb",oldDb);

        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }


}
