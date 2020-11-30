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

package org.apache.skywalking.aop.server.receiver.mesh;

import com.google.gson.JsonObject;
import org.apache.skywalking.apm.network.common.DetectPoint;
import org.apache.skywalking.apm.network.servicemesh.ServiceMeshMetric;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.receiver.sharing.server.CoreRegisterLinker;

/**
 * @author wusheng
 */
public class ServiceMeshMetricDataDecorator {
    private ServiceMeshMetric origin;
    private ServiceMeshMetric rebuiltData;
    private ServiceMeshMetric.Builder newDataBuilder;
    private int endpointId;

    public ServiceMeshMetricDataDecorator(ServiceMeshMetric origin) {
        this.origin = origin;
    }

    boolean tryMetaDataRegister() {
        int sourceServiceId = 0;
        int sourceServiceInstanceId = 0;
        int destServiceId = 0;
        int destServiceInstanceId = 0;
        boolean isRegistered = true;
        sourceServiceId = origin.getSourceServiceId();
        if (sourceServiceId == Const.NONE) {
            String sourceServiceName = origin.getSourceServiceName();
            // sourceServiceName is optional now,
            // which means only generate dest service traffic.
            if (!StringUtil.isEmpty(sourceServiceName)) {
                sourceServiceId = CoreRegisterLinker.getServiceInventoryRegister().getOrCreate(sourceServiceName, null);
                if (sourceServiceId != Const.NONE) {
                    getNewDataBuilder().setSourceServiceId(sourceServiceId);
                } else {
                    isRegistered = false;
                }
            }
            // No service name, service instance will be ignored too.
        }
        sourceServiceInstanceId = origin.getSourceServiceInstanceId();
        if (sourceServiceId != Const.NONE && sourceServiceInstanceId == Const.NONE) {
            sourceServiceInstanceId = CoreRegisterLinker.getServiceInstanceInventoryRegister()
                .getOrCreate(sourceServiceId, origin.getSourceServiceInstance(), origin.getSourceServiceInstance(),
                    origin.getEndTime(),
                    getOSInfoForMesh(origin.getSourceServiceInstance()));
            if (sourceServiceInstanceId != Const.NONE) {
                getNewDataBuilder().setSourceServiceInstanceId(sourceServiceInstanceId);
            } else {
                isRegistered = false;
            }
        }
        destServiceId = origin.getDestServiceId();
        if (destServiceId == Const.NONE) {
            destServiceId = CoreRegisterLinker.getServiceInventoryRegister().getOrCreate(origin.getDestServiceName(), null);
            if (destServiceId != Const.NONE) {
                getNewDataBuilder().setDestServiceId(destServiceId);
            } else {
                isRegistered = false;
            }
        }
        destServiceInstanceId = origin.getDestServiceInstanceId();
        if (destServiceId != Const.NONE && destServiceInstanceId == Const.NONE) {
            destServiceInstanceId = CoreRegisterLinker.getServiceInstanceInventoryRegister()
                .getOrCreate(destServiceId, origin.getDestServiceInstance(), origin.getDestServiceInstance(),
                    origin.getEndTime(),
                    getOSInfoForMesh(origin.getDestServiceInstance()));
            if (destServiceInstanceId != Const.NONE) {
                getNewDataBuilder().setDestServiceInstanceId(destServiceInstanceId);
            } else {
                isRegistered = false;
            }
        }
        String endpoint = origin.getEndpoint();

        // Service mesh doesn't register client side endpoint.
        DetectPoint point = origin.getDetectPoint();
        if (DetectPoint.server.equals(point)) {
            if (destServiceId != Const.NONE) {
                endpointId = CoreRegisterLinker.getEndpointInventoryRegister().getOrCreate(destServiceId, endpoint,
                    org.apache.skywalking.oap.server.core.source.DetectPoint.fromNetworkProtocolDetectPoint(point));
            }
        }

        return isRegistered;
    }

    public ServiceMeshMetric getMetric() {
        if (newDataBuilder != null) {
            if (rebuiltData == null) {
                rebuiltData = newDataBuilder.build();
            }
            return rebuiltData;
        } else {
            return origin;
        }
    }

    public int getEndpointId() {
        return endpointId;
    }

    private ServiceMeshMetric.Builder getNewDataBuilder() {
        if (newDataBuilder == null) {
            newDataBuilder = origin.toBuilder();
        }
        return newDataBuilder;
    }

    private JsonObject getOSInfoForMesh(String instanceName) {
        JsonObject properties = new JsonObject();
        properties.addProperty(ServiceInstanceInventory.PropertyUtil.HOST_NAME, instanceName);
        return properties;
    }
}
