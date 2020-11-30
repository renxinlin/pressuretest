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

package org.apache.skywalking.oap.server.core.analysis.metrics;

import lombok.*;
import org.apache.skywalking.oap.server.core.analysis.metrics.annotation.*;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;

/**
 * @author wusheng
 */
@MetricsFunction(functionName = "maxDouble")
public abstract class MaxDoubleMetrics extends Metrics implements DoubleValueHolder {

    protected static final String VALUE = "value";

    @Getter @Setter @Column(columnName = VALUE, isValue = true) private double value;

    @Entrance
    public final void combine(@SourceFrom double count) {
        if (count > this.value) {
            this.value = count;
        }
    }

    @Override public final void combine(Metrics metrics) {
        MaxDoubleMetrics maxDoubleMetrics = (MaxDoubleMetrics)metrics;
        combine(maxDoubleMetrics.value);
    }

    @Override public void calculate() {
    }
}
