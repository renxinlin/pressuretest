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

package org.apache.skywalking.oap.server.cluster.plugin.standalone;

import java.util.*;
import org.apache.skywalking.oap.server.core.cluster.*;
import org.apache.skywalking.oap.server.telemetry.api.TelemetryRelatedContext;

/**
 * A cluster manager simulator. Work in memory only. Also return the current instance.
 *
 * @author peng-yongsheng, Wu Sheng
 */
public class StandaloneManager implements ClusterNodesQuery, ClusterRegister {

    private volatile RemoteInstance remoteInstance;

    @Override public void registerRemote(RemoteInstance remoteInstance) {
        // core模块启动时注册自己 因为是Standalone模式,信息维护在内存即可
        this.remoteInstance = remoteInstance;
        this.remoteInstance.getAddress().setSelf(true);
        TelemetryRelatedContext.INSTANCE.setId("standalone");
    }

    @Override
    public List<RemoteInstance> queryRemoteNodes() {
        if (remoteInstance == null) {
            return new ArrayList(0);
        }
        ArrayList remoteList = new ArrayList(1);
        remoteList.add(remoteInstance);
        return remoteList;
    }
}
