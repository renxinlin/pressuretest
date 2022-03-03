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

package org.apache.skywalking.oap.server.core.storage.model;

import java.util.List;
import lombok.Getter;
import org.apache.skywalking.oap.server.core.analysis.Downsampling;

/**
 * @author peng-yongsheng
 */
@Getter
public class Model {

    private final String name;
    // 数据是否与时间相关
    private final boolean capableOfTimeSeries;
    // 设置时间向下采样的代码
    private final Downsampling downsampling;
    // 是否删除历史数据 是的话ttl或执行清理磁盘工作
    private final boolean deleteHistory;
    // 字段 以及字段的类型
    private final List<ModelColumn> columns;
    // 模型的全局唯一id
    private final int scopeId;
    private final boolean record;

    public Model(String name, List<ModelColumn> columns, boolean capableOfTimeSeries, boolean deleteHistory, int scopeId, Downsampling downsampling, boolean record) {
        this.columns = columns;
        this.capableOfTimeSeries = capableOfTimeSeries;
        this.downsampling = downsampling;
        this.deleteHistory = deleteHistory;
        this.scopeId = scopeId;
        this.name = ModelName.build(downsampling, name);
        this.record = record;
    }
}
