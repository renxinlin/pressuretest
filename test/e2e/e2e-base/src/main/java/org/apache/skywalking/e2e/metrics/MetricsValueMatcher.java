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

package org.apache.skywalking.e2e.metrics;

import org.apache.skywalking.e2e.verification.AbstractMatcher;

import java.util.Objects;

/**
 * @author kezhenxu94
 */
public class MetricsValueMatcher extends AbstractMatcher<MetricsValue> {
    private String value;

    @Override
    public void verify(MetricsValue metricsValue) {
        if (Objects.nonNull(getValue())) {
            doVerify(getValue(), metricsValue.getValue());
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MetricsValueMatcher{" +
            "value='" + value + '\'' +
            '}';
    }
}
