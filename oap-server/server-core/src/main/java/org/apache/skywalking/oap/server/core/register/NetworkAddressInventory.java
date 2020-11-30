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
import lombok.AccessLevel;
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

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.NETWORK_ADDRESS;

/**
 * @author peng-yongsheng
 */
@ScopeDeclaration(id = NETWORK_ADDRESS, name = "NetworkAddress")
@Stream(name = NetworkAddressInventory.INDEX_NAME, scopeId = DefaultScopeDefine.NETWORK_ADDRESS, builder = NetworkAddressInventory.Builder.class, processor = InventoryStreamProcessor.class)
public class NetworkAddressInventory extends RegisterSource {

    public static final String INDEX_NAME = "network_address_inventory";

    private static final String NAME = "name";
    private static final String NODE_TYPE = "node_type";

    @Setter @Getter @Column(columnName = NAME, matchQuery = true) private String name = Const.EMPTY_STRING;
    @Setter(AccessLevel.PRIVATE) @Getter(AccessLevel.PRIVATE) @Column(columnName = NODE_TYPE) private int nodeType;

    public void setNetworkAddressNodeType(NodeType nodeType) {
        this.nodeType = nodeType.value();
    }

    public NodeType getNetworkAddressNodeType() {
        return NodeType.get(this.nodeType);
    }

    public static String buildId(String networkAddress) {
        return networkAddress;
    }

    @Override public String id() {
        return buildId(name);
    }

    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        NetworkAddressInventory source = (NetworkAddressInventory)obj;
        if (!name.equals(source.getName()))
            return false;

        return true;
    }

    public NetworkAddressInventory getClone() {
        NetworkAddressInventory inventory = new NetworkAddressInventory();
        inventory.setSequence(getSequence());
        inventory.setRegisterTime(getRegisterTime());
        inventory.setHeartbeatTime(getHeartbeatTime());
        inventory.setLastUpdateTime(getLastUpdateTime());
        inventory.setName(name);
        inventory.setNodeType(nodeType);

        return inventory;
    }

    @Override public boolean combine(RegisterSource registerSource) {
        boolean isChanged = super.combine(registerSource);
        NetworkAddressInventory inventory = (NetworkAddressInventory)registerSource;

        if (this.nodeType != inventory.getNodeType() && inventory.getLastUpdateTime() >= this.getLastUpdateTime()) {
            setNodeType(inventory.getNodeType());
            return true;
        } else {
            return isChanged;
        }
    }

    @Override public RemoteData.Builder serialize() {
        RemoteData.Builder remoteBuilder = RemoteData.newBuilder();
        remoteBuilder.addDataIntegers(getSequence());
        remoteBuilder.addDataIntegers(getNodeType());

        remoteBuilder.addDataLongs(getRegisterTime());
        remoteBuilder.addDataLongs(getHeartbeatTime());
        remoteBuilder.addDataLongs(getLastUpdateTime());

        remoteBuilder.addDataStrings(Strings.isNullOrEmpty(name) ? Const.EMPTY_STRING : name);
        return remoteBuilder;
    }

    @Override public void deserialize(RemoteData remoteData) {
        setSequence(remoteData.getDataIntegers(0));
        setNodeType(remoteData.getDataIntegers(1));

        setRegisterTime(remoteData.getDataLongs(0));
        setHeartbeatTime(remoteData.getDataLongs(1));
        setLastUpdateTime(remoteData.getDataLongs(2));

        setName(remoteData.getDataStrings(0));
    }

    @Override public int remoteHashCode() {
        return 0;
    }

    public static class Builder implements StorageBuilder<NetworkAddressInventory> {

        @Override public NetworkAddressInventory map2Data(Map<String, Object> dbMap) {
            NetworkAddressInventory inventory = new NetworkAddressInventory();
            inventory.setSequence(((Number)dbMap.get(SEQUENCE)).intValue());
            inventory.setName((String)dbMap.get(NAME));
            inventory.setNodeType(((Number)dbMap.get(NODE_TYPE)).intValue());
            inventory.setRegisterTime(((Number)dbMap.get(REGISTER_TIME)).longValue());
            inventory.setHeartbeatTime(((Number)dbMap.get(HEARTBEAT_TIME)).longValue());
            inventory.setLastUpdateTime(((Number)dbMap.get(LAST_UPDATE_TIME)).longValue());
            return inventory;
        }

        @Override public Map<String, Object> data2Map(NetworkAddressInventory storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(SEQUENCE, storageData.getSequence());
            map.put(NAME, storageData.getName());
            map.put(NODE_TYPE, storageData.getNodeType());
            map.put(REGISTER_TIME, storageData.getRegisterTime());
            map.put(HEARTBEAT_TIME, storageData.getHeartbeatTime());
            map.put(LAST_UPDATE_TIME, storageData.getLastUpdateTime());
            return map;
        }
    }
}
