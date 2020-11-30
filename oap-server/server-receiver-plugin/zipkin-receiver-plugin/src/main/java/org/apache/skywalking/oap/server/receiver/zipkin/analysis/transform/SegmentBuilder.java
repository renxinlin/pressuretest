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

package org.apache.skywalking.oap.server.receiver.zipkin.analysis.transform;

import com.google.common.base.Strings;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.skywalking.apm.network.common.KeyStringValuePair;
import org.apache.skywalking.apm.network.language.agent.*;
import org.apache.skywalking.apm.network.language.agent.v2.*;
import org.apache.skywalking.oap.server.receiver.sharing.server.CoreRegisterLinker;
import org.apache.skywalking.oap.server.receiver.zipkin.analysis.*;
import org.apache.skywalking.oap.server.receiver.zipkin.analysis.data.SkyWalkingTrace;
import org.eclipse.jetty.util.StringUtil;
import zipkin2.*;

/**
 * @author wusheng
 */
public class SegmentBuilder {
    private Context context;
    private LinkedList<Segment> segments;
    private Map<String, ClientSideSpan> clientPartSpan;

    private SegmentBuilder() {
        segments = new LinkedList<>();
        context = new Context();
        clientPartSpan = new HashMap<>();
    }

    public static SkyWalkingTrace build(List<Span> traceSpans) throws Exception {
        SegmentBuilder builder = new SegmentBuilder();
        // This map groups the spans by their parent id, in order to assist to build tree.
        // key: parentId
        // value: span
        Map<String, List<Span>> childSpanMap = new HashMap<>();
        AtomicReference<Span> root = new AtomicReference<>();
        traceSpans.forEach(span -> {
            if (span.parentId() == null) {
                root.set(span);
            }
            List<Span> spanList = childSpanMap.get(span.parentId());
            if (spanList == null) {
                spanList = new LinkedList<>();
                spanList.add(span);
                childSpanMap.put(span.parentId(), spanList);
            } else {
                spanList.add(span);
            }
        });

        Span rootSpan = root.get();
        long timestamp = 0;
        if (rootSpan != null) {
            String applicationCode = rootSpan.localServiceName();
            // If root span doesn't include applicationCode, a.k.a local service name,
            // Segment can't be built
            // Ignore the whole trace.
            // :P Hope anyone could provide better solution.
            // Wu Sheng.
            if (!Strings.isNullOrEmpty(applicationCode)) {
                timestamp = rootSpan.timestampAsLong();
                builder.context.addApp(applicationCode, rootSpan.timestampAsLong() / 1000);

                SpanObjectV2.Builder rootSpanBuilder = builder.initSpan(null, null, rootSpan, true);
                builder.context.currentSegment().addSpan(rootSpanBuilder);
                builder.scanSpansFromRoot(rootSpanBuilder, rootSpan, childSpanMap);

                builder.segments.add(builder.context.removeApp());
            }
        }

        List<SegmentObject.Builder> segmentBuilders = new LinkedList<>();
        // microseconds -> million seconds
        long finalTimestamp = timestamp / 1000;
        builder.segments.forEach(segment -> {
            SegmentObject.Builder traceSegmentBuilder = segment.freeze();
            segmentBuilders.add(traceSegmentBuilder);
            CoreRegisterLinker.getServiceInventoryRegister().heartbeat(traceSegmentBuilder.getServiceId(), finalTimestamp);
            CoreRegisterLinker.getServiceInstanceInventoryRegister().heartbeat(traceSegmentBuilder.getServiceInstanceId(), finalTimestamp);
        });
        return new SkyWalkingTrace(builder.generateTraceOrSegmentId(), segmentBuilders);
    }

    private void scanSpansFromRoot(SpanObjectV2.Builder parentSegmentSpan, Span parent,
        Map<String, List<Span>> childSpanMap) throws Exception {
        String parentId = parent.id();
        // get child spans by parent span id
        List<Span> spanList = childSpanMap.get(parentId);
        if (spanList == null) {
            return;
        }
        for (Span childSpan : spanList) {
            String localServiceName = childSpan.localServiceName();
            boolean isNewApp = false;
            if (StringUtil.isNotBlank(localServiceName)) {
                if (context.isAppChanged(localServiceName)) {
                    isNewApp = true;
                }
            }

            try {
                if (isNewApp) {
                    context.addApp(localServiceName, childSpan.timestampAsLong() / 1000);
                }
                SpanObjectV2.Builder childSpanBuilder = initSpan(parentSegmentSpan, parent, childSpan, isNewApp);

                context.currentSegment().addSpan(childSpanBuilder);
                scanSpansFromRoot(childSpanBuilder, childSpan, childSpanMap);

            } finally {
                if (isNewApp) {
                    segments.add(context.removeApp());
                }
            }
        }
    }

    private SpanObjectV2.Builder initSpan(SpanObjectV2.Builder parentSegmentSpan, Span parentSpan, Span span,
        boolean isSegmentRoot) {
        SpanObjectV2.Builder spanBuilder = SpanObjectV2.newBuilder();
        spanBuilder.setSpanId(context.currentIDs().nextSpanId());
        if (isSegmentRoot) {
            // spanId = -1, means no parent span
            // spanId is considered unique, and from a positive sequence in each segment.
            spanBuilder.setParentSpanId(-1);
        }
        if (!isSegmentRoot && parentSegmentSpan != null) {
            spanBuilder.setParentSpanId(parentSegmentSpan.getSpanId());
        }
        Span.Kind kind = span.kind();
        String opName = Strings.isNullOrEmpty(span.name()) ? "-" : span.name();
        spanBuilder.setOperationName(opName);
        ClientSideSpan clientSideSpan;
        switch (kind) {
            case CLIENT:
                spanBuilder.setSpanType(SpanType.Exit);
                String peer = getPeer(parentSpan, span);
                if (peer != null) {
                    spanBuilder.setPeer(peer);
                }
                clientSideSpan = new ClientSideSpan(span, spanBuilder);
                clientPartSpan.put(span.id(), clientSideSpan);
                break;
            case SERVER:
                spanBuilder.setSpanType(SpanType.Entry);
                this.buildRef(spanBuilder, span, parentSegmentSpan, parentSpan);
                break;
            case CONSUMER:
                spanBuilder.setSpanType(SpanType.Entry);
                this.buildRef(spanBuilder, span, parentSegmentSpan, parentSpan);
                break;
            case PRODUCER:
                spanBuilder.setSpanType(SpanType.Exit);
                peer = getPeer(parentSpan, span);
                if (peer != null) {
                    spanBuilder.setPeer(peer);
                }
                clientSideSpan = new ClientSideSpan(span, spanBuilder);
                clientPartSpan.put(span.id(), clientSideSpan);
                break;
            default:
                spanBuilder.setSpanType(SpanType.Local);
        }
        // microseconds in Zipkin -> milliseconds in SkyWalking
        long startTime = span.timestamp() / 1000;
        // Some implement of zipkin client not include duration field in its report
        // package when duration's value be 0ms, Causing a null pointer exception here.
        Long durationObj = span.duration();
        long duration = (durationObj == null) ? 0 : durationObj.longValue() / 1000;
        spanBuilder.setStartTime(startTime);
        spanBuilder.setEndTime(startTime + duration);

        span.tags().forEach((tagKey, tagValue) -> spanBuilder.addTags(
            KeyStringValuePair.newBuilder().setKey(tagKey).setValue(tagValue).build())
        );

        span.annotations().forEach(annotation ->
            spanBuilder.addLogs(Log.newBuilder().setTime(annotation.timestamp() / 1000).addData(
                KeyStringValuePair.newBuilder().setKey("zipkin.annotation").setValue(annotation.value()).build()
            ))
        );

        return spanBuilder;
    }

    private void buildRef(SpanObjectV2.Builder spanBuilder, Span span, SpanObjectV2.Builder parentSegmentSpan,
        Span parentSpan) {
        Segment parentSegment = context.parentSegment();
        if (parentSegment == null) {
            return;
        }
        Segment rootSegment = context.rootSegment();
        if (rootSegment == null) {
            return;
        }

        if (span.shared() != null && span.shared()) {
            // using same span id in client and server for RPC
            // SkyWalking will build both sides of span
            ClientSideSpan clientSideSpan = clientPartSpan.get(span.id());
            if (clientSideSpan != null) {
                // For the root span, there may be no ref, because of no parent.
                parentSegmentSpan = clientSideSpan.getBuilder();
                parentSpan = clientSideSpan.getSpan();
            }
        }

        String peer = getPeer(parentSpan, span);
        if (StringUtil.isBlank(peer)) {
            //The IP is the most important for building the ref at both sides.
            return;
        }

        SegmentReference.Builder refBuilder = SegmentReference.newBuilder();
        refBuilder.setEntryServiceInstanceId(rootSegment.builder().getServiceInstanceId());
        int endpointId = rootSegment.getEntryEndpointId();
        if (endpointId == 0) {
            refBuilder.setEntryEndpoint(rootSegment.getEntryEndpointName());
        } else {
            refBuilder.setEntryEndpointId(endpointId);
        }
        refBuilder.setEntryServiceInstanceId(rootSegment.builder().getServiceInstanceId());

        // parent ref info
        refBuilder.setNetworkAddress(peer);
        parentSegmentSpan.setPeer(refBuilder.getNetworkAddress());
        refBuilder.setParentServiceInstanceId(parentSegment.builder().getServiceInstanceId());
        refBuilder.setParentSpanId(parentSegmentSpan.getSpanId());
        refBuilder.setParentTraceSegmentId(parentSegment.builder().getTraceSegmentId());
        int parentEndpointId = parentSegment.getEntryEndpointId();
        if (parentEndpointId == 0) {
            refBuilder.setParentEndpoint(parentSegment.getEntryEndpointName());
        } else {
            refBuilder.setParentEndpointId(parentEndpointId);
        }
        refBuilder.setRefType(RefType.CrossProcess);

        spanBuilder.addRefs(refBuilder);
    }

    private String getPeer(Span parentSpan, Span childSpan) {
        String peer;

        Endpoint serverEndpoint = childSpan == null ? null : childSpan.localEndpoint();
        peer = endpoint2Peer(serverEndpoint);

        if (peer == null) {
            Endpoint clientEndpoint = parentSpan == null ? null : parentSpan.remoteEndpoint();
            peer = endpoint2Peer(clientEndpoint);
        }

        return peer;
    }

    private String endpoint2Peer(Endpoint endpoint) {
        String ip = null;
        Integer port = 0;

        if (endpoint != null) {
            if (!Strings.isNullOrEmpty(endpoint.ipv4())) {
                ip = endpoint.ipv4();
                port = endpoint.port();
            } else if (!Strings.isNullOrEmpty(endpoint.ipv6())) {
                ip = endpoint.ipv6();
                port = endpoint.port();
            }
        }
        if (ip == null) {
            return null;
        } else {
            return port == null || port == 0 ? ip : ip + ":" + port;
        }
    }

    /**
     * Context holds the values in build process.
     */
    private class Context {
        private LinkedList<Segment> segmentsStack = new LinkedList<>();

        private boolean isAppChanged(String applicationCode) {
            return !Strings.isNullOrEmpty(applicationCode) && !applicationCode.equals(currentIDs().applicationCode);
        }

        private Segment addApp(String serviceCode, long registerTime) throws Exception {
            int serviceId = waitForExchange(() ->
                    CoreRegisterLinker.getServiceInventoryRegister().getOrCreate(serviceCode, null),
                10
            );

            int serviceInstanceId = waitForExchange(() ->
                    CoreRegisterLinker.getServiceInstanceInventoryRegister().getOrCreate(serviceId, serviceCode, serviceCode,
                        registerTime, ZipkinTraceOSInfoBuilder.getOSInfoForZipkin(serviceCode)),
                10
            );

            Segment segment = new Segment(serviceCode, serviceId, serviceInstanceId);
            segmentsStack.add(segment);
            return segment;
        }

        private IDCollection currentIDs() {
            return segmentsStack.getLast().ids;
        }

        private Segment currentSegment() {
            return segmentsStack.getLast();
        }

        private Segment parentSegment() {
            if (segmentsStack.size() < 2) {
                return null;
            } else {
                return segmentsStack.get(segmentsStack.size() - 2);
            }

        }

        private Segment rootSegment() {
            if (segmentsStack.size() < 2) {
                return null;
            } else {
                return segmentsStack.getFirst();
            }
        }

        private Segment removeApp() {
            return segmentsStack.removeLast();
        }

        private int waitForExchange(Callable<Integer> callable, int retry) throws Exception {
            for (int i = 0; i < retry; i++) {
                Integer id = callable.call();
                if (id == 0) {
                    Thread.sleep(1000L);
                } else {
                    return id;
                }
            }
            throw new TimeoutException("ID exchange costs more than expected.");
        }
    }

    private class Segment {
        private SegmentObject.Builder segmentBuilder;
        private IDCollection ids;
        private int entryEndpointId = 0;
        private String entryEndpointName = null;
        private List<SpanObjectV2.Builder> spans;
        private long endTime = 0;

        private Segment(String serviceCode, int serviceId, int serviceInstanceId) {
            ids = new IDCollection(serviceCode, serviceId, serviceInstanceId);
            spans = new LinkedList<>();
            segmentBuilder = SegmentObject.newBuilder();
            segmentBuilder.setServiceId(serviceId);
            segmentBuilder.setServiceInstanceId(serviceInstanceId);
            segmentBuilder.setTraceSegmentId(generateTraceOrSegmentId());
        }

        private SegmentObject.Builder builder() {
            return segmentBuilder;
        }

        private void addSpan(SpanObjectV2.Builder spanBuilder) {
            String operationName = spanBuilder.getOperationName();
            if (entryEndpointId == 0 && !Strings.isNullOrEmpty(operationName)) {
                if (SpanType.Entry == spanBuilder.getSpanType()) {
                    if (!Strings.isNullOrEmpty(operationName)) {
                        entryEndpointName = operationName;
                    } else {
                        entryEndpointId = spanBuilder.getOperationNameId();
                    }
                }
            }

            // init by root span
            if (spanBuilder.getSpanId() == 1 && entryEndpointId == 0) {
                if (!Strings.isNullOrEmpty(operationName)) {
                    entryEndpointName = operationName;
                } else {
                    entryEndpointId = spanBuilder.getOperationNameId();
                }
            }

            spans.add(spanBuilder);
            if (spanBuilder.getEndTime() > endTime) {
                endTime = spanBuilder.getEndTime();
            }
        }

        public int getEntryEndpointId() {
            return entryEndpointId;
        }

        public String getEntryEndpointName() {
            return entryEndpointName;
        }

        private IDCollection ids() {
            return ids;
        }

        public SegmentObject.Builder freeze() {
            for (SpanObjectV2.Builder span : spans) {
                segmentBuilder.addSpans(span);
            }
            return segmentBuilder;
        }

        public long getEndTime() {
            return endTime;
        }
    }

    private class IDCollection {
        private String applicationCode;
        private int appId;
        private int instanceId;
        private int spanIdSeq;

        private IDCollection(String applicationCode, int appId, int instanceId) {
            this.applicationCode = applicationCode;
            this.appId = appId;
            this.instanceId = instanceId;
            this.spanIdSeq = 0;
        }

        private int nextSpanId() {
            return spanIdSeq++;
        }
    }

    private UniqueId generateTraceOrSegmentId() {
        return UniqueId.newBuilder()
            .addIdParts(ThreadLocalRandom.current().nextLong())
            .addIdParts(ThreadLocalRandom.current().nextLong())
            .addIdParts(ThreadLocalRandom.current().nextLong())
            .build();
    }

    private class ClientSideSpan {
        private Span span;
        private SpanObjectV2.Builder builder;

        public ClientSideSpan(Span span, SpanObjectV2.Builder builder) {
            this.span = span;
            this.builder = builder;
        }

        public Span getSpan() {
            return span;
        }

        public SpanObjectV2.Builder getBuilder() {
            return builder;
        }
    }
}
