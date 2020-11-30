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

package org.apache.skywalking.oap.server.core.storage;

import org.apache.skywalking.oap.server.core.register.RegisterSource;

/**
 * Entity register and ID generator.
 *
 * @author peng-yongsheng, wusheng
 */
public interface IRegisterLockDAO extends DAO {
    /**
     * This method is also executed by one thread in each oap instance, but in cluster environment, it could be executed
     * in concurrent way, so no `sync` in method level, but the implementation must make sure the return id is unique no
     * matter the cluster size.
     *
     * @param scopeId for the id. IDs at different scopes could be same, but unique in same scope.
     * @return Unique ID.
     */
    int getId(int scopeId, RegisterSource registerSource);
}
