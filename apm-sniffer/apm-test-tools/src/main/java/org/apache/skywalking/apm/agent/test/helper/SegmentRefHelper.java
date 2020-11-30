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


package org.apache.skywalking.apm.agent.test.helper;

import org.apache.skywalking.apm.agent.core.context.ids.ID;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;

public class SegmentRefHelper {
    public static String getPeerHost(TraceSegmentRef ref) {
        try {
            return FieldGetter.getValue(ref, "peerHost");
        } catch (Exception e) {
        }

        return null;
    }

    public static ID getTraceSegmentId(TraceSegmentRef ref) {
        try {
            return FieldGetter.getValue(ref, "traceSegmentId");
        } catch (Exception e) {
        }

        return null;
    }

    public static int getSpanId(TraceSegmentRef ref) {
        try {
            return FieldGetter.getValue(ref, "spanId");
        } catch (Exception e) {
        }

        return -1;
    }

    public static int getEntryServiceInstanceId(TraceSegmentRef ref) {
        try {
            return FieldGetter.getValue(ref, "entryServiceInstanceId");
        } catch (Exception e) {
        }

        return -1;
    }
}
