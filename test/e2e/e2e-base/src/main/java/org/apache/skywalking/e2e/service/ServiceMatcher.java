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

package org.apache.skywalking.e2e.service;

import org.apache.skywalking.e2e.verification.AbstractMatcher;

import java.util.Objects;

/**
 * A simple matcher to verify the given {@code Service} is expected
 *
 * @author kezhenxu94
 */
public class ServiceMatcher extends AbstractMatcher<Service> {

    private String key;
    private String label;

    @Override
    public void verify(final Service service) {
        if (Objects.nonNull(getKey())) {
            verifyKey(service);
        }

        if (Objects.nonNull(getLabel())) {
            verifyLabel(service);
        }
    }

    private void verifyKey(Service service) {
        final String expected = getKey();
        final String actual = service.getKey();

        doVerify(expected, actual);
    }

    private void verifyLabel(Service service) {
        final String expected = getLabel();
        final String actual = service.getLabel();

        doVerify(expected, actual);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "ServiceMatcher{" +
            "key='" + key + '\'' +
            ", label='" + label + '\'' +
            '}';
    }
}
