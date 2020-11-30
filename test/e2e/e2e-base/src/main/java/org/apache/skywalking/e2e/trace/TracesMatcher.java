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

package org.apache.skywalking.e2e.trace;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author kezhenxu94
 */
public class TracesMatcher {
    private List<TraceMatcher> traces;

    public TracesMatcher() {
        this.traces = new LinkedList<>();
    }

    public List<TraceMatcher> getTraces() {
        return traces;
    }

    public void setTraces(List<TraceMatcher> traces) {
        this.traces = traces;
    }

    public void verify(final List<Trace> traces) {
        assertThat(traces).hasSameSizeAs(this.traces);

        int size = this.traces.size();

        for (int i = 0; i < size; i++) {
            this.traces.get(i).verify(traces.get(i));
        }
    }

    /**
     * Verify the traces in a loose manner
     *
     * @param traces
     */
    public void verifyLoosely(final List<Trace> traces) {
        for (int i = 0; i < getTraces().size(); i++) {
            boolean matched = false;
            for (int j = 0; j < traces.size(); j++) {
                try {
                    getTraces().get(i).verify(traces.get(j));
                    matched = true;
                } catch (Throwable ignored) {
                }
            }
            if (!matched) {
                fail("Expected: %s\n Actual: %s", getTraces(), traces);
            }
        }
    }

    @Override
    public String toString() {
        return "TracesMatcher{" +
            "traces=" + traces +
            '}';
    }
}
