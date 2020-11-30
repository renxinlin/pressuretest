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

import java.io.IOException;
import java.sql.*;
import java.util.*;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.storage.*;
import org.apache.skywalking.oap.server.core.storage.model.ModelColumn;
import org.apache.skywalking.oap.server.core.storage.type.StorageDataType;
import org.apache.skywalking.oap.server.library.client.jdbc.JDBCClientException;
import org.apache.skywalking.oap.server.library.client.jdbc.hikaricp.JDBCHikariCPClient;
import org.apache.skywalking.oap.server.storage.plugin.jdbc.*;
import org.slf4j.*;

/**
 * @author wusheng, peng-yongsheng
 */
public class H2SQLExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(H2SQLExecutor.class);

    protected List<StorageData> getByIDs(JDBCHikariCPClient h2Client, String modelName, String[] ids,
        StorageBuilder storageBuilder) throws IOException {

        try (Connection connection = h2Client.getConnection()) {
            /*
             * Although H2 database or other database support createArrayOf and setArray operate.
             * But Mysql 5.1.44 driver doesn't.
             */
            String param = ArrayParamBuilder.build(ids);

            try (ResultSet rs = h2Client.executeQuery(connection, "SELECT * FROM " + modelName + " WHERE id in (" + param + ")")) {
                List<StorageData> storageDataList = new ArrayList<>();
                StorageData storageData;
                do {
                    storageData = toStorageData(rs, modelName, storageBuilder);
                    if (storageData != null) {
                        storageDataList.add(storageData);
                    }
                }
                while (storageData != null);

                return storageDataList;
            }
        } catch (SQLException | JDBCClientException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected StorageData getByID(JDBCHikariCPClient h2Client, String modelName, String id,
        StorageBuilder storageBuilder) throws IOException {
        try (Connection connection = h2Client.getConnection()) {
            try (ResultSet rs = h2Client.executeQuery(connection, "SELECT * FROM " + modelName + " WHERE id = ?", id)) {
                return toStorageData(rs, modelName, storageBuilder);
            }
        } catch (SQLException | JDBCClientException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected StorageData getByColumn(JDBCHikariCPClient h2Client, String modelName, String columnName, Object value,
        StorageBuilder storageBuilder) throws IOException {
        try (Connection connection = h2Client.getConnection()) {
            try (ResultSet rs = h2Client.executeQuery(connection, "SELECT * FROM " + modelName + " WHERE " + columnName + " = ?", value)) {
                return toStorageData(rs, modelName, storageBuilder);
            }
        } catch (SQLException | JDBCClientException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected StorageData toStorageData(ResultSet rs, String modelName, StorageBuilder storageBuilder) throws SQLException {
        if (rs.next()) {
            Map data = new HashMap();
            List<ModelColumn> columns = TableMetaInfo.get(modelName).getColumns();
            for (ModelColumn column : columns) {
                data.put(column.getColumnName().getName(), rs.getObject(column.getColumnName().getStorageName()));
            }
            return storageBuilder.map2Data(data);
        }
        return null;
    }

    protected int getEntityIDByID(JDBCHikariCPClient h2Client, String entityColumnName, String modelName, String id) {
        try (Connection connection = h2Client.getConnection()) {
            try (ResultSet rs = h2Client.executeQuery(connection, "SELECT " + entityColumnName + " FROM " + modelName + " WHERE ID=?", id)) {
                while (rs.next()) {
                    return rs.getInt(ServiceInstanceInventory.SEQUENCE);
                }
            }
        } catch (SQLException | JDBCClientException e) {
            logger.error(e.getMessage(), e);
        }
        return Const.NONE;
    }

    protected SQLExecutor getInsertExecutor(String modelName, StorageData metrics, StorageBuilder storageBuilder) throws IOException {
        Map<String, Object> objectMap = storageBuilder.data2Map(metrics);

        SQLBuilder sqlBuilder = new SQLBuilder("INSERT INTO " + modelName + " VALUES");
        List<ModelColumn> columns = TableMetaInfo.get(modelName).getColumns();
        List<Object> param = new ArrayList<>();
        sqlBuilder.append("(?,");
        param.add(metrics.id());
        for (int i = 0; i < columns.size(); i++) {
            ModelColumn column = columns.get(i);
            sqlBuilder.append("?");
            if (i != columns.size() - 1) {
                sqlBuilder.append(",");
            }

            Object value = objectMap.get(column.getColumnName().getName());
            if (value instanceof StorageDataType) {
                param.add(((StorageDataType)value).toStorageData());
            } else {
                param.add(value);
            }
        }
        sqlBuilder.append(")");

        return new SQLExecutor(sqlBuilder.toString(), param);
    }

    protected SQLExecutor getUpdateExecutor(String modelName, StorageData metrics, StorageBuilder storageBuilder) throws IOException {
        Map<String, Object> objectMap = storageBuilder.data2Map(metrics);

        SQLBuilder sqlBuilder = new SQLBuilder("UPDATE " + modelName + " SET ");
        List<ModelColumn> columns = TableMetaInfo.get(modelName).getColumns();
        List<Object> param = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            ModelColumn column = columns.get(i);
            sqlBuilder.append(column.getColumnName().getStorageName() + "= ?");
            if (i != columns.size() - 1) {
                sqlBuilder.append(",");
            }

            Object value = objectMap.get(column.getColumnName().getName());
            if (value instanceof StorageDataType) {
                param.add(((StorageDataType)value).toStorageData());
            } else {
                param.add(value);
            }
        }
        sqlBuilder.append(" WHERE id = ?");
        param.add(metrics.id());

        return new SQLExecutor(sqlBuilder.toString(), param);
    }
}
