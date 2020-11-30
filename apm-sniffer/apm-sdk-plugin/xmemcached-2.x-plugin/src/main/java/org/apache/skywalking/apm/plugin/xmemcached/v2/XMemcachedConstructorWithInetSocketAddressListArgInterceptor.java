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


package org.apache.skywalking.apm.plugin.xmemcached.v2;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * @author IluckySi
 */
public class XMemcachedConstructorWithInetSocketAddressListArgInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        StringBuilder memcachConnInfo = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<InetSocketAddress> inetSocketAddressList = (List<InetSocketAddress>)allArguments[0];
        for (InetSocketAddress inetSocketAddress : inetSocketAddressList) {
            String host = inetSocketAddress.getAddress().getHostAddress();
            int port = inetSocketAddress.getPort();
            memcachConnInfo.append(host).append(":").append(port).append(";");
        }
        int length = memcachConnInfo.length();
        if (length > 1) {
            memcachConnInfo = new StringBuilder(memcachConnInfo.substring(0, length - 1));
        }
        objInst.setSkyWalkingDynamicField(memcachConnInfo.toString());
    }
}
