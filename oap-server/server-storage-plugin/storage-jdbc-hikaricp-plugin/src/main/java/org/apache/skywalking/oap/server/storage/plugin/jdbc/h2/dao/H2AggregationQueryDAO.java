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

package org.apache.skywalking.oap.server.storage.plugin.jdbc.h2.dao;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.skywalking.oap.server.core.analysis.Downsampling;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.core.query.entity.Order;
import org.apache.skywalking.oap.server.core.query.entity.TopNEntity;
import org.apache.skywalking.oap.server.core.register.EndpointInventory;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.storage.model.ModelName;
import org.apache.skywalking.oap.server.core.storage.query.IAggregationQueryDAO;
import org.apache.skywalking.oap.server.library.client.jdbc.hikaricp.JDBCHikariCPClient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wusheng
 * @author panjuan
 */
public class H2AggregationQueryDAO implements IAggregationQueryDAO {

    @Getter(AccessLevel.PROTECTED)
    private JDBCHikariCPClient h2Client;

    public H2AggregationQueryDAO(JDBCHikariCPClient h2Client) {
        this.h2Client = h2Client;
    }

    @Override
    public List<TopNEntity> getServiceTopN(String indName, String valueCName, int topN, Downsampling downsampling,
        long startTB, long endTB, Order order) throws IOException {
        return topNQuery(indName, valueCName, topN, downsampling, startTB, endTB, order, null);
    }

    @Override public List<TopNEntity> getAllServiceInstanceTopN(String indName, String valueCName, int topN,
        Downsampling downsampling, long startTB, long endTB, Order order) throws IOException {
        return topNQuery(indName, valueCName, topN, downsampling, startTB, endTB, order, null);
    }

    @Override
    public List<TopNEntity> getServiceInstanceTopN(int serviceId, String indName, String valueCName,
        int topN, Downsampling downsampling, long startTB, long endTB, Order order) throws IOException {
        return topNQuery(indName, valueCName, topN, downsampling, startTB, endTB, order, (sql, conditions) -> {
            sql.append(" and ").append(ServiceInstanceInventory.SERVICE_ID).append("=?");
            conditions.add(serviceId);
        });
    }

    @Override
    public List<TopNEntity> getAllEndpointTopN(String indName, String valueCName, int topN, Downsampling downsampling,
        long startTB, long endTB, Order order) throws IOException {
        return topNQuery(indName, valueCName, topN, downsampling, startTB, endTB, order, null);
    }

    @Override public List<TopNEntity> getEndpointTopN(int serviceId, String indName, String valueCName,
        int topN, Downsampling downsampling, long startTB, long endTB, Order order) throws IOException {
        return topNQuery(indName, valueCName, topN, downsampling, startTB, endTB, order, (sql, conditions) -> {
            sql.append(" and ").append(EndpointInventory.SERVICE_ID).append("=?");
            conditions.add(serviceId);
        });
    }

    public List<TopNEntity> topNQuery(String indName, String valueCName, int topN, Downsampling downsampling,
                                      long startTB, long endTB, Order order, AppendCondition appender) throws IOException {
        String indexName = ModelName.build(downsampling, indName);
        StringBuilder sql = new StringBuilder();
        List<Object> conditions = new ArrayList<>(10);
        sql.append("select * from (select avg(").append(valueCName).append(") value,").append(Metrics.ENTITY_ID).append(" from ")
                .append(indexName).append(" where ");
        this.setTimeRangeCondition(sql, conditions, startTB, endTB);
        if (appender != null) {
            appender.append(sql, conditions);
        }
        sql.append(" group by ").append(Metrics.ENTITY_ID);
        sql.append(") order by value ").append(order.equals(Order.ASC) ? "asc" : "desc").append(" limit ").append(topN);
        List<TopNEntity> topNEntities = new ArrayList<>();
        try (Connection connection = h2Client.getConnection()) {
            try (ResultSet resultSet = h2Client.executeQuery(connection, sql.toString(), conditions.toArray(new Object[0]))) {
            
                try {
                    while (resultSet.next()) {
                        TopNEntity topNEntity = new TopNEntity();
                        topNEntity.setId(resultSet.getString(Metrics.ENTITY_ID));
                        topNEntity.setValue(resultSet.getLong("value"));
                        topNEntities.add(topNEntity);
                    }
                } catch (SQLException e) {
                    throw new IOException(e);
                }
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return topNEntities;
    }

    protected void setTimeRangeCondition(StringBuilder sql, List<Object> conditions, long startTimestamp,
        long endTimestamp) {
        sql.append(Metrics.TIME_BUCKET).append(" >= ? and ").append(Metrics.TIME_BUCKET).append(" <= ?");
        conditions.add(startTimestamp);
        conditions.add(endTimestamp);
    }

    protected interface AppendCondition {
        void append(StringBuilder sql, List<Object> conditions);
    }
}
