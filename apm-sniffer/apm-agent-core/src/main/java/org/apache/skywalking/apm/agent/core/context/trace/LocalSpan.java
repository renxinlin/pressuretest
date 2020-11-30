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


package org.apache.skywalking.apm.agent.core.context.trace;

/**
 * The <code>LocalSpan</code> represents a normal tracing point, such as a local method.
 *
 * @author wusheng
 */
public class LocalSpan extends AbstractTracingSpan {

    public LocalSpan(int spanId, int parentSpanId, int operationId) {
        super(spanId, parentSpanId, operationId);
    }

    public LocalSpan(int spanId, int parentSpanId, String operationName) {
        super(spanId, parentSpanId, operationName);
    }

    @Override
    public LocalSpan tag(String key, String value) {
        super.tag(key, value);
        return this;
    }

    @Override
    public LocalSpan log(Throwable t) {
        super.log(t);
        return this;
    }

    @Override public boolean isEntry() {
        return false;
    }

    @Override public boolean isExit() {
        return false;
    }

    @Override public AbstractSpan setPeer(String remotePeer) {
        return this;
    }
}
