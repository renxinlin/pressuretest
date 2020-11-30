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

package org.apache.skywalking.oap.server.core.analysis.manual.database;

import java.util.*;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.topn.TopN;
import org.apache.skywalking.oap.server.core.analysis.worker.TopNStreamProcessor;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;

/**
 * Database TopN statement, including Database SQL statement, mongoDB and Redis commands.
 *
 * @author wusheng
 */
@Stream(name = TopNDatabaseStatement.INDEX_NAME, scopeId = DefaultScopeDefine.DATABASE_SLOW_STATEMENT, builder = TopNDatabaseStatement.Builder.class, processor = TopNStreamProcessor.class)
public class TopNDatabaseStatement extends TopN {

    public static final String INDEX_NAME = "top_n_database_statement";

    @Setter private String id;

    @Override public String id() {
        return id;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TopNDatabaseStatement statement = (TopNDatabaseStatement)o;
        return getServiceId() == statement.getServiceId();
    }

    @Override public int hashCode() {
        return Objects.hash(getServiceId());
    }

    public static class Builder implements StorageBuilder<TopNDatabaseStatement> {

        @Override public TopNDatabaseStatement map2Data(Map<String, Object> dbMap) {
            TopNDatabaseStatement statement = new TopNDatabaseStatement();
            statement.setStatement((String)dbMap.get(STATEMENT));
            statement.setTraceId((String)dbMap.get(TRACE_ID));
            statement.setLatency(((Number)dbMap.get(LATENCY)).longValue());
            statement.setServiceId(((Number)dbMap.get(SERVICE_ID)).intValue());
            statement.setTimeBucket(((Number)dbMap.get(TIME_BUCKET)).longValue());
            return statement;
        }

        @Override public Map<String, Object> data2Map(TopNDatabaseStatement storageData) {
            Map<String, Object> map = new HashMap<>();
            map.put(STATEMENT, storageData.getStatement());
            map.put(TRACE_ID, storageData.getTraceId());
            map.put(LATENCY, storageData.getLatency());
            map.put(SERVICE_ID, storageData.getServiceId());
            map.put(TIME_BUCKET, storageData.getTimeBucket());
            return map;
        }
    }
}
