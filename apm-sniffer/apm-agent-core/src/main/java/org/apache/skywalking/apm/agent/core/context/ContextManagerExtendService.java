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

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.boot.*;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;

/**
 * @author wusheng
 */
@DefaultImplementor
public class ContextManagerExtendService implements BootService {
    @Override public void prepare() {

    }

    @Override public void boot() {

    }

    @Override public void onComplete() {

    }

    @Override public void shutdown() {

    }

    public AbstractTracerContext createTraceContext(String operationName, boolean forceSampling) {
        forceSampling = true; // 开启强制采用 renxl 只需要改一个变量 达到永远强制span生成 和TracingContext
        AbstractTracerContext context ;
        int suffixIdx = operationName.lastIndexOf(".");
        // 如果配置名单忽略该endpoint/操作资源 则忽略
        if (suffixIdx > -1 && Config.Agent.IGNORE_SUFFIX.contains(operationName.substring(suffixIdx))) {
            context = new IgnoredTracerContext();
        } else {

            SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
            // 如果强制采样则采样  如果不强制采样 则按照采样机制判断是否采样
            if (forceSampling || samplingService.trySampling()) {
                context = new TracingContext();
                TraceSegment segment = ((TracingContext) context).getSegment();
//                segment.setIgnore(true); 永远不上报
            } else {
                //
                context = new IgnoredTracerContext();
            }
        }

        return context;
    }
}
