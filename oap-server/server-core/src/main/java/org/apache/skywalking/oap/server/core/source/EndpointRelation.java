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

package org.apache.skywalking.oap.server.core.source;

import lombok.*;
import org.apache.skywalking.oap.server.core.Const;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.ENDPOINT_RELATION;

/**
 * @author peng-yongsheng
 */
@ScopeDeclaration(id = ENDPOINT_RELATION, name = "EndpointRelation")
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class EndpointRelation extends Source {

    @Override public int scope() {
        return DefaultScopeDefine.ENDPOINT_RELATION;
    }

    @Override public String getEntityId() {
        return String.valueOf(endpointId) + Const.ID_SPLIT + String.valueOf(childEndpointId);
    }

    @Getter @Setter private int endpointId;
    @Getter @Setter private String endpoint;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "service_id") private int serviceId;
    @Getter @Setter private String serviceName;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "service_instance_id") private int serviceInstanceId;
    @Getter @Setter private String serviceInstanceName;

    @Getter @Setter private int childEndpointId;
    @Getter @Setter private String childEndpoint;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "child_service_id") private int childServiceId;
    @Getter @Setter private String childServiceName;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "child_service_instance_id") private int childServiceInstanceId;
    @Getter @Setter private String childServiceInstanceName;

    @Getter @Setter private int componentId;
    @Getter @Setter private int rpcLatency;
    @Getter @Setter private boolean status;
    @Getter @Setter private int responseCode;
    @Getter @Setter private RequestType type;
    @Getter @Setter private DetectPoint detectPoint;
}

