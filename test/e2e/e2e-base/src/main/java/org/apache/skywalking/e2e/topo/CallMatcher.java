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

package org.apache.skywalking.e2e.topo;

import org.apache.skywalking.e2e.verification.AbstractMatcher;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kezhenxu94
 */
public class CallMatcher extends AbstractMatcher<Call> {
    private String id;
    private String source;
    private List<String> detectPoints;
    private String target;

    @Override
    public void verify(final Call call) {
        if (Objects.nonNull(getId())) {
            final String expected = this.getId();
            final String actual = call.getId();

            doVerify(expected, actual);
        }

        if (Objects.nonNull(getSource())) {
            final String expected = this.getSource();
            final String actual = call.getSource();

            doVerify(expected, actual);
        }

        if (Objects.nonNull(getDetectPoints())) {
            assertThat(getDetectPoints()).hasSameSizeAs(call.getDetectPoints());
            int size = getDetectPoints().size();

            for (int i = 0; i < size; i++) {
                final String expected = getDetectPoints().get(i);
                final String actual = call.getDetectPoints().get(i);

                doVerify(expected, actual);
            }
        }

        if (Objects.nonNull(getTarget())) {
            final String expected = this.getTarget();
            final String actual = call.getTarget();

            doVerify(expected, actual);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getDetectPoints() {
        return detectPoints;
    }

    public void setDetectPoints(List<String> detectPoints) {
        this.detectPoints = detectPoints;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return "CallMatcher{" +
            "id='" + id + '\'' +
            ", source='" + source + '\'' +
            ", detectPoints=" + detectPoints +
            ", target='" + target + '\'' +
            '}';
    }
}
