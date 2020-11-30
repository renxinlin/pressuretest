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

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.base64.Base64;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ids.DistributedTraceId;
import org.apache.skywalking.apm.agent.core.context.ids.ID;
import org.apache.skywalking.apm.agent.core.context.ids.PropagatedTraceId;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.apache.skywalking.apm.util.StringUtil;

import java.io.Serializable;
import java.util.List;

/**
 *Sample value
 * value值示例：
 *
 * 1.2343.234234234|1|1|1|#127.0.0.1:8080|#/portal/|#/testEntrySpan|1.2343.234234234
 * 1.2343.234234234|1|1|1|#127.0.0.1:8080|#/portal/|1038|1.2343.234234234
 *
 * 分布式追踪要解决的一个重要问题，就是跨进程调用链连接的问题，ContextCarrier的概念就是为了解决这种场景。【renxl】
 *
 * 当发生网络调用 跨进程调用
 *
 *
 * 创建一个空的ContextCarrier
 * 通过ContextManager#createExitSpan方法创建一个ExitSpan，或者使用ContextManager#inject，在过程中传入并初始化ContextCarrier
 * 将ContextCarrier中所有元素放入请求头（如：HTTP头）或消息正文（如 Kafka） 【这一步需要对不同的拦截器做不同的实现】
 * 而且这一步往往是配对的  比如 mq p-c端   dubbo p-c端 http-tomcat端等
 * ContextCarrier随请求传输到服务端
 * 服务端收到后，转换为新的ContextCarrier
 * 通过ContestManager#createEntrySpan方法创建EntrySpan，或者使用ContextManager#extract，建立分布式调用关联
 *
 *
 *
 *
 *
 *
 * {@link ContextCarrier} is a data carrier of {@link TracingContext}. It holds the snapshot (current state) of {@link
 * TracingContext}.
 * <p>
 * Created by wusheng on 2017/2/17.
 */
public class ContextCarrier implements Serializable {
    /**
     * {@link TraceSegment#traceSegmentId}
     */
    private ID traceSegmentId;

    /**
     * id of parent span. It is unique in parent trace segment.
     */
    private int spanId = -1;

    /**
     * id of parent application instance, it's the id assigned by collector.
     */
    private int parentServiceInstanceId = DictionaryUtil.nullValue();

    /**
     * id of first application instance in this distributed trace, it's the id assigned by collector.
     */
    private int entryServiceInstanceId = DictionaryUtil.nullValue();

    /**
     * peer(ipv4s/ipv6/hostname + port) of the server, from client side.
     */
    private String peerHost;

    /**
     * Operation/Service name of the first one in this distributed trace. This name may be compressed to an integer.
     */
    private String entryEndpointName;

    /**
     * Operation/Service name of the parent one in this distributed trace. This name may be compressed to an integer.
     */
    private String parentEndpointName;

    /**
     * 全链路压测标志
     */
    private boolean pressureTest;

    /**
     * {@link DistributedTraceId}, also known as TraceId
     */
    private DistributedTraceId primaryDistributedTraceId;

    public CarrierItem items() {
        CarrierItemHead head;
        if (Config.Agent.ACTIVE_V2_HEADER && Config.Agent.ACTIVE_V1_HEADER) {
            SW3CarrierItem carrierItem = new SW3CarrierItem(this, null);
            SW6CarrierItem sw6CarrierItem = new SW6CarrierItem(this, carrierItem);
            head = new CarrierItemHead(sw6CarrierItem);
        } else if (Config.Agent.ACTIVE_V2_HEADER) {
            SW6CarrierItem sw6CarrierItem = new SW6CarrierItem(this, null);
            head = new CarrierItemHead(sw6CarrierItem);
        } else if (Config.Agent.ACTIVE_V1_HEADER) {
            SW3CarrierItem carrierItem = new SW3CarrierItem(this, null);
            head = new CarrierItemHead(carrierItem);
        } else {
            throw new IllegalArgumentException("At least active v1 or v2 header.");
        }
        return head;
    }

    /**
     * Serialize this {@link ContextCarrier} to a {@link String}, with '|' split.
     *
     * @return the serialization string.
     */
    String serialize(HeaderVersion version) {
        if (this.isValid(version)) {
            if (HeaderVersion.v1.equals(version)) {
                if (Config.Agent.ACTIVE_V1_HEADER) {
                    return StringUtil.join('|',
                            this.getTraceSegmentId().encode(),
                            this.getSpanId() + "",
                            this.getParentServiceInstanceId() + "",
                            this.getEntryServiceInstanceId() + "",
                            this.getPeerHost(),
                            this.getEntryEndpointName(),
                            this.getParentEndpointName(),
                            this.getPrimaryDistributedTraceId().encode(),
                            Boolean.toString(this.isPressureTest())

                    );
                } else {
                    return "";
                }
            } else {
                if (Config.Agent.ACTIVE_V2_HEADER) {
                    return StringUtil.join('-',
                            "1",
                            Base64.encode(this.getPrimaryDistributedTraceId().encode()),
                            Base64.encode(this.getTraceSegmentId().encode()),
                            this.getSpanId() + "",
                            this.getParentServiceInstanceId() + "",
                            this.getEntryServiceInstanceId() + "",
                            Base64.encode(this.getPeerHost()),
                            Base64.encode(this.getEntryEndpointName()),
                            Base64.encode(this.getParentEndpointName()),
                            Boolean.toString(this.isPressureTest()));
                } else {
                    return "";
                }
            }
        } else {
            return "";
        }
    }

    /**
     * Initialize fields with the given text.
     *
     * @param text carries {@link #traceSegmentId} and {@link #spanId}, with '|' split.
     */
    ContextCarrier deserialize(String text, HeaderVersion version) {
        if (text != null) {
            // if this carrier is initialized by v1 or v2, don't do deserialize again for performance.
            if (this.isValid(HeaderVersion.v1) || this.isValid(HeaderVersion.v2)) {
                return this;
            }
            if (HeaderVersion.v1.equals(version)) {
                String[] parts = text.split("\\|", 8);
                if (parts.length == 8) {
                    try {
                        // 实例ID  线程号  时间戳+线程内的自增序列
                        this.traceSegmentId = new ID(parts[0]);
                        // 一个整数，在trace segment内唯一，从0开始自增
                        this.spanId = Integer.parseInt(parts[1]);
                        // 父级应用节点的应用实例ID。
                        this.parentServiceInstanceId = Integer.parseInt(parts[2]);
                        // 入口应用节点的应用实例ID。如：在一个分布式链路A->B->C中，此字段为A应用的实例ID。
                        this.entryServiceInstanceId = Integer.parseInt(parts[3]);
                        // 服务端的Peer Host或Peer Id
                        this.peerHost = parts[4];
                        // 调用链入口节点的应用实例下,入口Span的operation name或id。
                        this.entryEndpointName = parts[5];
                        // 调用链父级节点的应用实例下,入口Span的operation name或id。
                        this.parentEndpointName = parts[6];
                        // 分布式链路ID一般是整个调用链的全局唯一ID。如果针对批量消费情况，这个ID是批量中，第一个生产者的trace ID。此ID生成规则和Trace Segment Id一致，由三个Long型数字构成
                        this.primaryDistributedTraceId = new PropagatedTraceId(parts[7]);
                        this.pressureTest = Boolean.parseBoolean(parts[8]);
                    } catch (NumberFormatException e) {

                    }
                }
            } else if (HeaderVersion.v2.equals(version)) {
                String[] parts = text.split("\\-", 9);
                if (parts.length == 9) {
                    try {
                        // parts[0] is sample flag, always trace if header exists.
                        this.primaryDistributedTraceId = new PropagatedTraceId(Base64.decode2UTFString(parts[1]));
                        this.traceSegmentId = new ID(Base64.decode2UTFString(parts[2]));
                        this.spanId = Integer.parseInt(parts[3]);
                        this.parentServiceInstanceId = Integer.parseInt(parts[4]);
                        this.entryServiceInstanceId = Integer.parseInt(parts[5]);
                        this.peerHost = Base64.decode2UTFString(parts[6]);
                        this.entryEndpointName = Base64.decode2UTFString(parts[7]);
                        this.parentEndpointName = Base64.decode2UTFString(parts[8]);
                        this.pressureTest = Boolean.parseBoolean(parts[9]);

                    } catch (NumberFormatException e) {

                    }
                }
            } else {
                throw new IllegalArgumentException("Unimplemented header version." + version);
            }
        }
        return this;
    }

    public boolean isValid() {
        return isValid(HeaderVersion.v2) || isValid(HeaderVersion.v1);
    }

    /**
     * Make sure this {@link ContextCarrier} has been initialized.
     *
     * @return true for unbroken {@link ContextCarrier} or no-initialized. Otherwise, false;
     */
    boolean isValid(HeaderVersion version) {
        if (HeaderVersion.v1.equals(version)) {
            return traceSegmentId != null
                    && traceSegmentId.isValid()
                    && getSpanId() > -1
                    && parentServiceInstanceId != DictionaryUtil.nullValue()
                    && entryServiceInstanceId != DictionaryUtil.nullValue()
                    && !StringUtil.isEmpty(peerHost)
                    && !StringUtil.isEmpty(entryEndpointName)
                    && !StringUtil.isEmpty(parentEndpointName)
                    && primaryDistributedTraceId != null;
        } else if (HeaderVersion.v2.equals(version)) {
            return traceSegmentId != null
                    && traceSegmentId.isValid()
                    && getSpanId() > -1
                    && parentServiceInstanceId != DictionaryUtil.nullValue()
                    && entryServiceInstanceId != DictionaryUtil.nullValue()
                    && !StringUtil.isEmpty(peerHost)
                    && primaryDistributedTraceId != null;
        } else {
            throw new IllegalArgumentException("Unimplemented header version." + version);
        }
    }

    public String getEntryEndpointName() {
        return entryEndpointName;
    }

    void setEntryEndpointName(String entryEndpointName) {
        this.entryEndpointName = '#' + entryEndpointName;
    }

    void setEntryEndpointId(int entryOperationId) {
        this.entryEndpointName = entryOperationId + "";
    }

    void setParentEndpointId(int parentOperationId) {
        this.parentEndpointName = parentOperationId + "";
    }

    public ID getTraceSegmentId() {
        return traceSegmentId;
    }

    void setTraceSegmentId(ID traceSegmentId) {
        this.traceSegmentId = traceSegmentId;
    }

    public int getSpanId() {
        return spanId;
    }

    void setSpanId(int spanId) {
        this.spanId = spanId;
    }

    public int getParentServiceInstanceId() {
        return parentServiceInstanceId;
    }

    void setParentServiceInstanceId(int parentServiceInstanceId) {
        this.parentServiceInstanceId = parentServiceInstanceId;
    }

    public String getPeerHost() {
        return peerHost;
    }

    void setPeerHost(String peerHost) {
        this.peerHost = '#' + peerHost;
    }

    void setPeerId(int peerId) {
        this.peerHost = peerId + "";
    }

    public DistributedTraceId getDistributedTraceId() {
        return primaryDistributedTraceId;
    }

    public void setDistributedTraceIds(List<DistributedTraceId> distributedTraceIds) {
        this.primaryDistributedTraceId = distributedTraceIds.get(0);
    }

    private DistributedTraceId getPrimaryDistributedTraceId() {
        return primaryDistributedTraceId;
    }

    public String getParentEndpointName() {
        return parentEndpointName;
    }

    void setParentEndpointName(String parentEndpointName) {
        this.parentEndpointName = '#' + parentEndpointName;
    }

    public int getEntryServiceInstanceId() {
        return entryServiceInstanceId;
    }

    public void setEntryServiceInstanceId(int entryServiceInstanceId) {
        this.entryServiceInstanceId = entryServiceInstanceId;
    }

    public boolean isPressureTest() {
        return pressureTest;
    }

    public void setPressureTest(boolean pressureTest) {
        this.pressureTest = pressureTest;
    }

    public enum HeaderVersion {
        v1, v2
    }
}
