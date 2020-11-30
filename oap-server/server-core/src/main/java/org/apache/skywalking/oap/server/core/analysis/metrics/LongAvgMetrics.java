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
import org.apache.skywalking.oap.server.core.query.sql.Function;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;

/**
 * @author peng-yongsheng
 */
@MetricsFunction(functionName = "longAvg")
public abstract class LongAvgMetrics extends Metrics implements LongValueHolder {

    protected static final String SUMMATION = "summation";
    protected static final String COUNT = "count";
    protected static final String VALUE = "value";

    @Getter @Setter @Column(columnName = SUMMATION) private long summation;
    @Getter @Setter @Column(columnName = COUNT) private long count;
    @Getter @Setter @Column(columnName = VALUE, isValue = true, function = Function.Avg) private long value;

    @Entrance
    public final void combine(@SourceFrom long summation, @ConstOne long count) {
        this.summation += summation;
        this.count += count;
    }

    @Override public final void combine(Metrics metrics) {
        LongAvgMetrics longAvgMetrics = (LongAvgMetrics)metrics;
        combine(longAvgMetrics.summation, longAvgMetrics.count);
    }

    @Override public final void calculate() {
        this.value = this.summation / this.count;
    }
}
