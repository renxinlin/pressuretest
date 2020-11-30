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

package org.apache.skywalking.oap.server.core.alarm.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wusheng
 */
public class Threshold {
    private static final Logger logger = LoggerFactory.getLogger(Threshold.class);

    private String alarmRuleName;
    private final String threshold;
    private int intThreshold;
    private double doubleThreadhold;
    private long longThreshold;

    public Threshold(String alarmRuleName, String threshold) {
        this.alarmRuleName = alarmRuleName;
        this.threshold = threshold;
    }

    public int getIntThreshold() {
        return intThreshold;
    }

    public double getDoubleThreadhold() {
        return doubleThreadhold;
    }

    public long getLongThreshold() {
        return longThreshold;
    }

    public void setType(MetricsValueType type) {
        try {
            switch (type) {
                case INT:
                    intThreshold = Integer.parseInt(threshold);
                    break;
                case LONG:
                    longThreshold = Long.parseLong(threshold);
                    break;
                case DOUBLE:
                    doubleThreadhold = Double.parseDouble(threshold);
                    break;
            }
        } catch (NumberFormatException e) {
            logger.warn("Alarm rule {} threshold doesn't match the metrics type, expected type: {}", alarmRuleName, type);
        }
    }
}
