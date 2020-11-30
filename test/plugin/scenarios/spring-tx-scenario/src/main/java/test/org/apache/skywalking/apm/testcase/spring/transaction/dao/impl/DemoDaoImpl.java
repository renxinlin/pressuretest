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
package test.org.apache.skywalking.apm.testcase.spring.transaction.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import test.org.apache.skywalking.apm.testcase.spring.transaction.dao.DemoDao;

/**
 * @author zhaoyuguang
 */
@Repository
public class DemoDaoImpl implements DemoDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public int insert(String name) {
        return jdbcTemplate.update("insert into `test`.`table_demo`(name) values(?)", name);
    }

}
