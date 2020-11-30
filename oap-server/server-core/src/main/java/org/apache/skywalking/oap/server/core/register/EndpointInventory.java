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

package org.apache.skywalking.oap.server.core.register;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.register.worker.InventoryStreamProcessor;
import org.apache.skywalking.oap.server.core.remote.grpc.proto.RemoteData;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.source.ScopeDeclaration;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.ENDPOINT_INVENTORY;

/**
 * @author peng-yongsheng
 */
@ScopeDeclaration(id = ENDPOINT_INVENTORY, name = "EndpointInventory")
@Stream(name = EndpointInventory.INDEX_NAME, scopeId = DefaultScopeDefine.ENDPOINT_INVENTORY, builder = EndpointInventory.Builder.class, processor = InventoryStreamProcessor.class)
public class EndpointInventory extends RegisterSource {

    public static final String INDEX_NAME = "endpoint_inventory";

    public static final String SERVICE_ID = "service_id";
    public static final String NAME = "name";
    public static final String DETECT_POINT = "detect_point";

    @Setter @Getter @Column(columnName = SERVICE_ID) private int serviceId;
    @Setter @Getter @Column(columnName = NAME, matchQuery = true) private String name = Const.EMPTY_STRING;
    @Setter @Getter @Column(columnName = DETECT_POINT) private int detectPoint;

    public static String buildId(int serviceId, String endpointName, int detectPoint) {
        return serviceId + Const.ID_SPLIT + endpointName + Const.ID_SPLIT + detectPoint;
    }

    @Override public String id() {
        return buildId(serviceId, name, detectPoint);
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + serviceId;
        result = 31 * result + name.hashCode();
        result = 31 * result + detectPoint;
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        EndpointInventory source = (EndpointInventory)obj;
        if (serviceId != source.getServiceId())
            return false;
        if (!name.equals(source.getName()))
            return false;
        if (detectPoint != source.getDetectPoint())
            return false;

        return true;
    }

    @Override public RemoteData.Builder serialize() {
        RemoteData.Builder remoteBuilder = RemoteData.newBuilder();
        remoteBuilder.addDataIntegers(getSequence());
        remoteBuilder.addDataIntegers(serviceId);
        remoteBuilder.addDataIntegers(detectPoint);

        remoteBuilder.addDataLongs(getRegisterTime());
        remoteBuilder.addDataLongs(getHeartbeatTime());
        remoteBuilder.addDataLongs(getLastUpdateTime());

        remoteBuilder.addDataStrings(Strings.isNullOrEmpty(name) ? Const.EMPTY_STRING : name);
        return remoteBuilder;
    }

    @Override public void deserialize(RemoteData remoteData) {
        setSequence(remoteData.getDataIntegers(0));
        setServiceId(remoteData.getDataIntegers(1));
        setDetectPoint(remoteData.getDataIntegers(2));

        setRegisterTime(remoteData.getDataLongs(0));
        setHeartbeatTime(remoteData.getDataLongs(1));
        setLastUpdateTime(remoteData.getDataLongs(2));

        setName(remoteData.getDataStrings(0));
    }

    @Override public int remoteHashCode() {
        return 0;
    }

    public static class Builder implements StorageBuilder<EndpointInventory> {

        @Override public EndpointInventory map2Data(Map<String, Object> dbMap) {
            EndpointInventory inventory = new EndpointInventory();
            inventory.setSequence(((Number)dbMap.get(SEQUENCE)).intValue());
            inventory.setServiceId(((Number)dbMap.get(SERVICE_ID)).intValue());
            inventory.setName((String)dbMap.get(NAME));
            inventory.setDetectPoint(((Number)dbMap.get(DETECT_POINT)).intValue());
            inventory.setRegisterTime(((Number)dbMap.get(REGISTER_TIME)).longValue());
            inventory.setHeartbeatTime(((Number)dbMap.get(HEARTBEAT_TIME)).longValue());
            inventory.setLastUpdateTime(((Number)dbMap.get(LAST_UPDATE_TIME)).longValue());
            return inventory;
        }

        @Override public Map<String, Object> data2Map(EndpointInventory storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(SEQUENCE, storageData.getSequence());
            map.put(SERVICE_ID, storageData.getServiceId());
            map.put(NAME, storageData.getName());
            map.put(DETECT_POINT, storageData.getDetectPoint());
            map.put(REGISTER_TIME, storageData.getRegisterTime());
            map.put(HEARTBEAT_TIME, storageData.getHeartbeatTime());
            map.put(LAST_UPDATE_TIME, storageData.getLastUpdateTime());
            return map;
        }
    }
}
