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

package org.apache.skywalking.oap.server.storage.plugin.jdbc.mysql;

import java.sql.*;
import org.apache.skywalking.oap.server.core.analysis.manual.segment.SegmentRecord;
import org.apache.skywalking.oap.server.core.analysis.metrics.IntKeyLongValueHashMap;
import org.apache.skywalking.oap.server.core.register.RegisterSource;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.StorageException;
import org.apache.skywalking.oap.server.core.storage.model.*;
import org.apache.skywalking.oap.server.library.client.Client;
import org.apache.skywalking.oap.server.library.client.jdbc.JDBCClientException;
import org.apache.skywalking.oap.server.library.client.jdbc.hikaricp.JDBCHikariCPClient;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.storage.plugin.jdbc.SQLBuilder;
import org.apache.skywalking.oap.server.storage.plugin.jdbc.h2.dao.H2TableInstaller;
import org.slf4j.*;

import static org.apache.skywalking.oap.server.core.source.DefaultScopeDefine.*;

/**
 * Extend H2TableInstaller but match MySQL SQL syntax.
 *
 * @author wusheng
 */
public class MySQLTableInstaller extends H2TableInstaller {

    private static final Logger logger = LoggerFactory.getLogger(MySQLTableInstaller.class);

    public MySQLTableInstaller(ModuleManager moduleManager) {
        super(moduleManager);
        /*
         * Override column because the default column names in core have syntax conflict with MySQL.
         */
        this.overrideColumnName("precision", "cal_precision");
        this.overrideColumnName("match", "match_num");
    }

    @Override protected void createTable(Client client, Model model) throws StorageException {
        super.createTable(client, model);
        JDBCHikariCPClient jdbcHikariCPClient = (JDBCHikariCPClient)client;
        this.createIndexes(jdbcHikariCPClient, model);
    }

    @Override
    protected String getColumnType(Model model, ColumnName name, Class<?> type) {
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return "INT";
        } else if (Long.class.equals(type) || long.class.equals(type)) {
            return "BIGINT";
        } else if (Double.class.equals(type) || double.class.equals(type)) {
            return "DOUBLE";
        } else if (String.class.equals(type)) {
            if (DefaultScopeDefine.SEGMENT == model.getScopeId()) {
                if (name.getName().equals(SegmentRecord.TRACE_ID) || name.getName().equals(SegmentRecord.SEGMENT_ID))
                    return "VARCHAR(300)";
                if (name.getName().equals(SegmentRecord.DATA_BINARY)) {
                    return "MEDIUMTEXT";
                }
            }
            return "VARCHAR(2000)";
        } else if (IntKeyLongValueHashMap.class.equals(type)) {
            return "MEDIUMTEXT";
        } else if (byte[].class.equals(type)) {
            return "MEDIUMTEXT";
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + type.getName());
        }
    }

    protected void createIndexes(JDBCHikariCPClient client, Model model) throws StorageException {
        switch (model.getScopeId()) {
            case SERVICE_INVENTORY:
            case SERVICE_INSTANCE_INVENTORY:
            case NETWORK_ADDRESS:
            case ENDPOINT_INVENTORY:
                createInventoryIndexes(client, model);
                return;
            case SEGMENT:
                createSegmentIndexes(client, model);
                return;
            case ALARM:
                createAlarmIndexes(client, model);
                return;
            default:
                createIndexesForAllMetrics(client, model);
        }
    }

    private void createIndexesForAllMetrics(JDBCHikariCPClient client, Model model) throws StorageException {
        try (Connection connection = client.getConnection()) {
            SQLBuilder tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_TIME_BUCKET ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.TIME_BUCKET).append(")");
            createIndex(client, connection, model, tableIndexSQL);
        } catch (JDBCClientException | SQLException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void createAlarmIndexes(JDBCHikariCPClient client, Model model) throws StorageException {
        try (Connection connection = client.getConnection()) {
            SQLBuilder tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_TIME_BUCKET ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.TIME_BUCKET).append(")");
            createIndex(client, connection, model, tableIndexSQL);
        } catch (JDBCClientException | SQLException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void createSegmentIndexes(JDBCHikariCPClient client, Model model) throws StorageException {
        try (Connection connection = client.getConnection()) {
            SQLBuilder tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_TRACE_ID ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.TRACE_ID).append(")");
            createIndex(client, connection, model, tableIndexSQL);

            tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_ENDPOINT_ID ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.ENDPOINT_ID).append(")");
            createIndex(client, connection, model, tableIndexSQL);

            tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_LATENCY ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.LATENCY).append(")");
            createIndex(client, connection, model, tableIndexSQL);

            tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_TIME_BUCKET ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(SegmentRecord.TIME_BUCKET).append(")");
            createIndex(client, connection, model, tableIndexSQL);
        } catch (JDBCClientException | SQLException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void createInventoryIndexes(JDBCHikariCPClient client, Model model) throws StorageException {
        try (Connection connection = client.getConnection()) {
            SQLBuilder tableIndexSQL = new SQLBuilder("CREATE UNIQUE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_SEQ ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(RegisterSource.SEQUENCE).append(")");
            createIndex(client, connection, model, tableIndexSQL);

            tableIndexSQL = new SQLBuilder("CREATE INDEX ");
            tableIndexSQL.append(model.getName().toUpperCase()).append("_TIME ");
            tableIndexSQL.append("ON ").append(model.getName()).append("(").append(RegisterSource.HEARTBEAT_TIME).append(", ").append(RegisterSource.REGISTER_TIME).append(")");
            createIndex(client, connection, model, tableIndexSQL);
        } catch (JDBCClientException | SQLException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void createIndex(JDBCHikariCPClient client, Connection connection, Model model,
        SQLBuilder indexSQL) throws JDBCClientException {
        if (logger.isDebugEnabled()) {
            logger.debug("create index for table {}, sql: {} ", model.getName(), indexSQL.toStringInNewLine());
        }
        client.execute(connection, indexSQL.toString());
    }
}
