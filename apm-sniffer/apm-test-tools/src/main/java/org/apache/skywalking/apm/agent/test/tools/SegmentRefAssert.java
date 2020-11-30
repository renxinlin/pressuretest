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


package org.apache.skywalking.apm.agent.test.tools;

import org.apache.skywalking.apm.agent.core.context.trace.TraceSegmentRef;
import org.apache.skywalking.apm.agent.test.helper.SegmentRefHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SegmentRefAssert {
    public static void assertSegmentId(TraceSegmentRef ref, String segmentId) {
        assertThat(SegmentRefHelper.getTraceSegmentId(ref).toString(), is(segmentId));
    }

    public static void assertSpanId(TraceSegmentRef ref, int spanId) {
        assertThat(SegmentRefHelper.getSpanId(ref), is(spanId));
    }

    public static void assertPeerHost(TraceSegmentRef ref, String peerHost) {
        assertThat(SegmentRefHelper.getPeerHost(ref), is(peerHost));
    }

    public static void assertEntryApplicationInstanceId(TraceSegmentRef ref, int entryApplicationInstanceID) {
        assertThat(SegmentRefHelper.getEntryServiceInstanceId(ref), is(entryApplicationInstanceID));
    }

}
