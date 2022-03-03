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

import org.apache.skywalking.oap.server.library.module.Service;

/**
 * @author peng-yongsheng
 *
 *
 * IBatchDAO【批量处理】
 * IMetricsDAO【增删改查metrics】
 * IRecordDAO【增删改查record】
 * IRegisterDAO[增删改查 注册数据]
 * IRegisterLockDAO【register需要的全局锁功能】
 */
public interface DAO extends Service {
}
