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

package org.apache.skywalking.apm.testcase.shardingsphere.service.utility.config;

import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DataSourceUtil {
    
    private static final String DEFAULT_SCHEMA = "";

    private static final Map<String, DataSource> datasourceMap = new HashMap<>();

    public static void createDataSource(final String dataSourceName) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName("org.h2.Driver");
        result.setUrl(String.format("jdbc:h2:mem:%s", dataSourceName));
        result.setUsername("sa");
        result.setPassword("");
        datasourceMap.put(dataSourceName, result);
    }

    public static DataSource getDataSource(final String dataSourceName) {
        return datasourceMap.get(dataSourceName);
    }
    
    public static void createSchema(final String dataSourceName) {
        String sql = "CREATE SCHEMA " + dataSourceName;
        try (Connection connection = getDataSource(DEFAULT_SCHEMA).getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (final SQLException ignored) {
        }
    }
}
