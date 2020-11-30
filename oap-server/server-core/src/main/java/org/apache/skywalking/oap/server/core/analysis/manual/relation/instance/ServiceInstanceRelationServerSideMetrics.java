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

package org.apache.skywalking.oap.server.core.analysis.manual.relation.instance;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.manual.RelationDefineUtil;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.core.analysis.worker.MetricsStreamProcessor;
import org.apache.skywalking.oap.server.core.remote.grpc.proto.RemoteData;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.annotation.IDColumn;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangwei
 */
@Stream(name = ServiceInstanceRelationServerSideMetrics.INDEX_NAME, scopeId = DefaultScopeDefine.SERVICE_INSTANCE_RELATION, builder = ServiceInstanceRelationServerSideMetrics.Builder.class, processor = MetricsStreamProcessor.class)
public class ServiceInstanceRelationServerSideMetrics extends Metrics {

    public static final String INDEX_NAME = "service_instance_relation_server_side";
    public static final String SOURCE_SERVICE_ID = "source_service_id";
    public static final String SOURCE_SERVICE_INSTANCE_ID = "source_service_instance_id";
    public static final String DEST_SERVICE_ID = "dest_service_id";
    public static final String DEST_SERVICE_INSTANCE_ID = "dest_service_instance_id";
    public static final String COMPONENT_ID = "component_id";

    @Setter @Getter @Column(columnName = SOURCE_SERVICE_ID) private int sourceServiceId;
    @Setter @Getter @Column(columnName = SOURCE_SERVICE_INSTANCE_ID) @IDColumn private int sourceServiceInstanceId;
    @Setter @Getter @Column(columnName = DEST_SERVICE_ID) private int destServiceId;
    @Setter @Getter @Column(columnName = DEST_SERVICE_INSTANCE_ID) @IDColumn private int destServiceInstanceId;
    @Setter @Getter @Column(columnName = COMPONENT_ID) @IDColumn private int componentId;
    @Setter(AccessLevel.PRIVATE) @Getter @Column(columnName = ENTITY_ID) @IDColumn private String entityId;

    @Override
    public String id() {
        String splitJointId = String.valueOf(getTimeBucket());
        splitJointId += Const.ID_SPLIT + RelationDefineUtil.buildEntityId(
                new RelationDefineUtil.RelationDefine(sourceServiceInstanceId, destServiceInstanceId, componentId)
        );
        return splitJointId;
    }

    public void buildEntityId() {
        String splitJointId = String.valueOf(sourceServiceInstanceId);
        splitJointId += Const.ID_SPLIT + destServiceInstanceId;
        splitJointId += Const.ID_SPLIT + componentId;
        entityId = splitJointId;
    }

    @Override
    public void combine(Metrics metrics) {

    }

    @Override
    public void calculate() {

    }

    @Override
    public Metrics toHour() {
        ServiceInstanceRelationServerSideMetrics metrics = new ServiceInstanceRelationServerSideMetrics();
        metrics.setTimeBucket(toTimeBucketInHour());
        metrics.setSourceServiceId(getSourceServiceId());
        metrics.setSourceServiceInstanceId(getSourceServiceInstanceId());
        metrics.setDestServiceId(getDestServiceId());
        metrics.setDestServiceInstanceId(getDestServiceInstanceId());
        metrics.setComponentId(getComponentId());
        metrics.setEntityId(getEntityId());
        return metrics;
    }

    @Override
    public Metrics toDay() {
        ServiceInstanceRelationServerSideMetrics metrics = new ServiceInstanceRelationServerSideMetrics();
        metrics.setTimeBucket(toTimeBucketInDay());
        metrics.setSourceServiceId(getSourceServiceId());
        metrics.setSourceServiceInstanceId(getSourceServiceInstanceId());
        metrics.setDestServiceId(getDestServiceId());
        metrics.setDestServiceInstanceId(getDestServiceInstanceId());
        metrics.setComponentId(getComponentId());
        metrics.setEntityId(getEntityId());
        return metrics;
    }

    @Override
    public Metrics toMonth() {
        ServiceInstanceRelationServerSideMetrics metrics = new ServiceInstanceRelationServerSideMetrics();
        metrics.setTimeBucket(toTimeBucketInMonth());
        metrics.setSourceServiceId(getSourceServiceId());
        metrics.setSourceServiceInstanceId(getSourceServiceInstanceId());
        metrics.setDestServiceId(getDestServiceId());
        metrics.setDestServiceInstanceId(getDestServiceInstanceId());
        metrics.setComponentId(getComponentId());
        metrics.setEntityId(getEntityId());
        return metrics;
    }

    @Override
    public int remoteHashCode() {
        int result = sourceServiceInstanceId;
        result = 31 * result + destServiceInstanceId;
        result = 31 * result + componentId;
        return result;
    }

    @Override
    public void deserialize(RemoteData remoteData) {
        setEntityId(remoteData.getDataStrings(0));

        setSourceServiceId(remoteData.getDataIntegers(0));
        setSourceServiceInstanceId(remoteData.getDataIntegers(1));
        setDestServiceId(remoteData.getDataIntegers(2));
        setDestServiceInstanceId(remoteData.getDataIntegers(3));
        setComponentId(remoteData.getDataIntegers(4));

        setTimeBucket(remoteData.getDataLongs(0));
    }

    @Override
    public RemoteData.Builder serialize() {
        RemoteData.Builder remoteBuilder = RemoteData.newBuilder();

        remoteBuilder.addDataIntegers(getSourceServiceId());
        remoteBuilder.addDataIntegers(getSourceServiceInstanceId());
        remoteBuilder.addDataIntegers(getDestServiceId());
        remoteBuilder.addDataIntegers(getDestServiceInstanceId());
        remoteBuilder.addDataIntegers(getComponentId());

        remoteBuilder.addDataStrings(getEntityId());

        remoteBuilder.addDataLongs(getTimeBucket());
        return remoteBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceInstanceRelationServerSideMetrics that = (ServiceInstanceRelationServerSideMetrics) o;

        if (sourceServiceInstanceId != that.sourceServiceInstanceId) return false;
        if (destServiceInstanceId != that.destServiceInstanceId) return false;
        return componentId == that.componentId;
    }

    @Override
    public int hashCode() {
        int result = sourceServiceInstanceId;
        result = 31 * result + destServiceInstanceId;
        result = 31 * result + componentId;
        return result;
    }

    public static class Builder implements StorageBuilder<ServiceInstanceRelationServerSideMetrics> {

        @Override
        public ServiceInstanceRelationServerSideMetrics map2Data(Map<String, Object> dbMap) {
            ServiceInstanceRelationServerSideMetrics metrics = new ServiceInstanceRelationServerSideMetrics();
            metrics.setEntityId((String) dbMap.get(ENTITY_ID));
            metrics.setSourceServiceId(((Number)dbMap.get(SOURCE_SERVICE_ID)).intValue());
            metrics.setSourceServiceInstanceId(((Number) dbMap.get(SOURCE_SERVICE_INSTANCE_ID)).intValue());
            metrics.setDestServiceId(((Number)dbMap.get(DEST_SERVICE_ID)).intValue());
            metrics.setDestServiceInstanceId(((Number) dbMap.get(DEST_SERVICE_INSTANCE_ID)).intValue());
            metrics.setComponentId(((Number) dbMap.get(COMPONENT_ID)).intValue());
            metrics.setTimeBucket(((Number) dbMap.get(TIME_BUCKET)).longValue());
            return metrics;
        }

        @Override
        public Map<String, Object> data2Map(ServiceInstanceRelationServerSideMetrics storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(ENTITY_ID, storageData.getEntityId());
            map.put(SOURCE_SERVICE_ID, storageData.getSourceServiceId());
            map.put(SOURCE_SERVICE_INSTANCE_ID, storageData.getSourceServiceInstanceId());
            map.put(DEST_SERVICE_ID, storageData.getDestServiceId());
            map.put(DEST_SERVICE_INSTANCE_ID, storageData.getDestServiceInstanceId());
            map.put(COMPONENT_ID, storageData.getComponentId());
            map.put(TIME_BUCKET, storageData.getTimeBucket());
            return map;
        }
    }
}
