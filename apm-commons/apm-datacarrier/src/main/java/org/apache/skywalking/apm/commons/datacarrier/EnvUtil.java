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

package org.apache.skywalking.apm.commons.datacarrier;

/**
 * Read value from system env.
 *
 * @author wusheng
 */
public class EnvUtil {
    public static int getInt(String envName, int defaultValue) {
        int value = defaultValue;
        String envValue = System.getenv(envName);
        if (envValue != null) {
            try {
                value = Integer.parseInt(envValue);
            } catch (NumberFormatException e) {

            }
        }
        return value;
    }

    public static long getLong(String envName, long defaultValue) {
        long value = defaultValue;
        String envValue = System.getenv(envName);
        if (envValue != null) {
            try {
                value = Long.parseLong(envValue);
            } catch (NumberFormatException e) {

            }
        }
        return value;
    }
}
