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

package org.apache.skywalking.e2e.service.endpoint;

import org.apache.skywalking.e2e.verification.AbstractMatcher;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kezhenxu94
 */
public class EndpointsMatcher extends AbstractMatcher<Endpoints> {
    private List<EndpointMatcher> endpoints;

    public List<EndpointMatcher> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointMatcher> endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    public void verify(Endpoints endpoints) {
        if (Objects.nonNull(getEndpoints())) {
            assertThat(endpoints.getEndpoints()).hasSameSizeAs(getEndpoints());

            int size = getEndpoints().size();

            for (int i = 0; i < size; i++) {
                getEndpoints().get(i).verify(endpoints.getEndpoints().get(i));
            }
        }
    }

    @Override
    public String toString() {
        return "EndpointsMatcher{" +
            "endpoints=" + endpoints +
            '}';
    }
}
