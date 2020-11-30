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

package org.apache.skywalking.oap.server.core.storage.cache;

import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.storage.DAO;

import java.util.List;

/**
 * @author peng-yongsheng
 */
public interface IServiceInstanceInventoryCacheDAO extends DAO {

    ServiceInstanceInventory get(int serviceInstanceId);

    int getServiceInstanceId(int serviceId, String uuid);

    int getServiceInstanceId(int serviceId, int addressId);

    List<ServiceInstanceInventory> loadLastUpdate(long lastUpdateTime);
}
