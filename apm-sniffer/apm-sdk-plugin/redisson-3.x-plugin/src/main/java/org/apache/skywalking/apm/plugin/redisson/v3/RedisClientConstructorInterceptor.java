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

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.pt.FlagValue;
import org.redisson.client.RedisClient;

/**
 * RedisClient is the link between RedisConnection and ConnectionManager.
 * to enhance RedisClient for bring peer(the cluster configuration information) in ConnectionManager to RedisConnection.
 *
 * @author zhaoyuguang
 */
public class RedisClientConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        boolean pt = FlagValue.isPt();
        // 压测数据偏移
//        if(pt){
//            RedisClient redisClient = (RedisClient) objInst;
//            int dbOffset = (redisClient.getConfig().getDatabase() + 1) % 16;
//            redisClient.getConfig().setDatabase(dbOffset);
//        }
    }
}
