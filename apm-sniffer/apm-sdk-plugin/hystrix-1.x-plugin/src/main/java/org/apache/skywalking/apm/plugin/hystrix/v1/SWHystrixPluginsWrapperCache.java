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

package org.apache.skywalking.apm.plugin.hystrix.v1;

/**
 * {@link SWHystrixPluginsWrapperCache} record the {@link SWExecutionHookWrapper} and {@link SWHystrixConcurrencyStrategyWrapper} object for
 * storing in EnhancedInstance#dynamicField together.
 *
 * @author chenpengfei
 */
public class SWHystrixPluginsWrapperCache {
    private volatile SWExecutionHookWrapper swExecutionHookWrapper;
    private volatile SWHystrixConcurrencyStrategyWrapper swHystrixConcurrencyStrategyWrapper;

    public SWExecutionHookWrapper getSwExecutionHookWrapper() {
        return swExecutionHookWrapper;
    }

    public void setSwExecutionHookWrapper(SWExecutionHookWrapper swExecutionHookWrapper) {
        this.swExecutionHookWrapper = swExecutionHookWrapper;
    }

    public SWHystrixConcurrencyStrategyWrapper getSwHystrixConcurrencyStrategyWrapper() {
        return swHystrixConcurrencyStrategyWrapper;
    }

    public void setSwHystrixConcurrencyStrategyWrapper(SWHystrixConcurrencyStrategyWrapper swHystrixConcurrencyStrategyWrapper) {
        this.swHystrixConcurrencyStrategyWrapper = swHystrixConcurrencyStrategyWrapper;
    }
}
