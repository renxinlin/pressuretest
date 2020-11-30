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

package org.apache.skywalking.oap.server.configuration.zookeeper;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

/**
 * @author zhaoyuguang
 */
@Getter
@Setter
public class ZookeeperServerSettings extends ModuleConfig {
    private String nameSpace = "/default";
    private String hostPort;
    private int baseSleepTimeMs = 1000;
    private int maxRetries = 3;
    private int period = 60;

    @Override
    public String toString() {
        return "ZookeeperServerSettings(nameSpace=" + this.getNameSpace()
                + ", hostPort=" + this.getHostPort()
                + ", baseSleepTimeMs=" + this.getBaseSleepTimeMs()
                + ", maxRetries=" + this.getMaxRetries()
                + ", period=" + this.getPeriod() + ")";
    }
}
