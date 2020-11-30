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

package org.apache.skywalking.apm.agent.core.dictionary;

import io.netty.util.internal.ConcurrentSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.skywalking.apm.agent.core.logging.api.*;
import org.apache.skywalking.apm.network.common.DetectPoint;
import org.apache.skywalking.apm.network.register.v2.*;

import static org.apache.skywalking.apm.agent.core.conf.Config.Dictionary.ENDPOINT_NAME_BUFFER_SIZE;

/**
 * @author wusheng
 */
public enum EndpointNameDictionary {
    INSTANCE;
    private static final ILog logger = LogManager.getLogger(EndpointNameDictionary.class);

    private Map<OperationNameKey, Integer> endpointDictionary = new ConcurrentHashMap<OperationNameKey, Integer>();
    private Set<OperationNameKey> unRegisterEndpoints = new ConcurrentSet<OperationNameKey>();

    public PossibleFound findOrPrepare4Register(int serviceId, String endpointName) {
        return find0(serviceId, endpointName, true);
    }

    public PossibleFound findOnly(int serviceId, String endpointName) {
        return find0(serviceId, endpointName, false);
    }

    private PossibleFound find0(int serviceId, String endpointName,
        boolean registerWhenNotFound) {
        if (endpointName == null || endpointName.length() == 0) {
            return new NotFound();
        }
        OperationNameKey key = new OperationNameKey(serviceId, endpointName);
        Integer operationId = endpointDictionary.get(key);
        if (operationId != null) {
            return new Found(operationId);
        } else {
            if (registerWhenNotFound &&
                endpointDictionary.size() + unRegisterEndpoints.size() < ENDPOINT_NAME_BUFFER_SIZE) {
                unRegisterEndpoints.add(key);
            }
            return new NotFound();
        }
    }

    public void syncRemoteDictionary(
        RegisterGrpc.RegisterBlockingStub serviceNameDiscoveryServiceBlockingStub) {
        if (unRegisterEndpoints.size() > 0) {
            Endpoints.Builder builder = Endpoints.newBuilder();
            for (OperationNameKey operationNameKey : unRegisterEndpoints) {
                Endpoint endpoint = Endpoint.newBuilder()
                    .setServiceId(operationNameKey.getServiceId())
                    .setEndpointName(operationNameKey.getEndpointName())
                    .setFrom(DetectPoint.server)
                    .build();
                builder.addEndpoints(endpoint);
            }
            EndpointMapping serviceNameMappingCollection = serviceNameDiscoveryServiceBlockingStub.doEndpointRegister(builder.build());
            if (serviceNameMappingCollection.getElementsCount() > 0) {
                for (EndpointMappingElement element : serviceNameMappingCollection.getElementsList()) {
                    OperationNameKey key = new OperationNameKey(
                        element.getServiceId(),
                        element.getEndpointName());
                    unRegisterEndpoints.remove(key);
                    endpointDictionary.put(key, element.getEndpointId());
                }
            }
        }
    }

    public void clear() {
        endpointDictionary.clear();
    }

    private class OperationNameKey {
        private int serviceId;
        private String endpointName;

        public OperationNameKey(int serviceId, String endpointName) {
            this.serviceId = serviceId;
            this.endpointName = endpointName;
        }

        public int getServiceId() {
            return serviceId;
        }

        public String getEndpointName() {
            return endpointName;
        }

        @Override public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            OperationNameKey key = (OperationNameKey)o;

            boolean isServiceEndpointMatch = false;
            if (serviceId == key.serviceId && endpointName.equals(key.endpointName)) {
                isServiceEndpointMatch = true;
            }
            return isServiceEndpointMatch;
        }

        @Override public int hashCode() {
            int result = serviceId;
            result = 31 * result + endpointName.hashCode();
            return result;
        }


        @Override public String toString() {
            return "OperationNameKey{" +
                "serviceId=" + serviceId +
                ", endpointName='" + endpointName + '\'' +
                '}';
        }
    }
}
