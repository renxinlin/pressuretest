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

/**
 * @author peng-yongsheng
 * 代表一条数据
 *
 *
 * StorageBuilder map和下述对象的相互转换
 * Record  trace等数据【AlarmRecord SegmentRecord(服务 以及服务实例 同步数据endpointName以及NetworkAddress) TopNDatabaseStatement】
 * Metrics 指标数据
 * RegisterSource 注册数据
 */
public interface StorageData {
    String id();
}
