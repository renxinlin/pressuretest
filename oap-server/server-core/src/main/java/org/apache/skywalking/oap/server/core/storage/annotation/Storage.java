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
 */

package org.apache.skywalking.oap.server.core.storage.annotation;

import lombok.Getter;
import org.apache.skywalking.oap.server.core.analysis.Downsampling;

/**
 * @author peng-yongsheng
 */
@Getter
public class Storage {
    // 模型的元信息类 表示一个es索引的名称 是否时间序列  是否能删除历史数据  是否开启降采样
    private final String modelName;
    private final boolean capableOfTimeSeries;
    private final boolean deleteHistory;
    private final Downsampling downsampling;

    public Storage(String modelName, boolean capableOfTimeSeries, boolean deleteHistory, Downsampling downsampling) {
        this.modelName = modelName;
        this.capableOfTimeSeries = capableOfTimeSeries;
        this.deleteHistory = deleteHistory;
        this.downsampling = downsampling;
    }
}
