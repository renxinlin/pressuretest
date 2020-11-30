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

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.SERVICE_INSTANCE_RELATION;

/**
 * @author peng-yongsheng
 */
@ScopeDeclaration(id = SERVICE_INSTANCE_RELATION, name = "ServiceInstanceRelation")
@ScopeDefaultColumn.VirtualColumnDefinition(fieldName = "entityId", columnName = "entity_id", isID = true, type = String.class)
public class ServiceInstanceRelation extends Source {

    @Override public int scope() {
        return DefaultScopeDefine.SERVICE_INSTANCE_RELATION;
    }

    @Override public String getEntityId() {
        return String.valueOf(sourceServiceInstanceId) + Const.ID_SPLIT + String.valueOf(destServiceInstanceId);
    }

    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "source_service_instance_id") private int sourceServiceInstanceId;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "source_service_id") private int sourceServiceId;
    @Getter @Setter private String sourceServiceName;
    @Getter @Setter private String sourceServiceInstanceName;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "dest_service_instance_id") private int destServiceInstanceId;
    @Getter @Setter @ScopeDefaultColumn.DefinedByField(columnName = "dest_service_id") private int destServiceId;
    @Getter @Setter private String destServiceName;
    @Getter @Setter private String destServiceInstanceName;
    @Getter @Setter private String endpoint;
    @Getter @Setter private int componentId;
    @Getter @Setter private int latency;
    @Getter @Setter private boolean status;
    @Getter @Setter private int responseCode;
    @Getter @Setter private RequestType type;
    @Getter @Setter private DetectPoint detectPoint;
}
