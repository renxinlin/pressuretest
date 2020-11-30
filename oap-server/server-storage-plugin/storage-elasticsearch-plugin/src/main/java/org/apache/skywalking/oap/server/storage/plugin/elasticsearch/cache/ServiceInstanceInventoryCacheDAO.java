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

package org.apache.skywalking.oap.server.storage.plugin.elasticsearch.cache;

import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.register.RegisterSource;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.storage.cache.IServiceInstanceInventoryCacheDAO;
import org.apache.skywalking.oap.server.library.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.oap.server.library.util.BooleanUtils;
import org.apache.skywalking.oap.server.storage.plugin.elasticsearch.base.EsDAO;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peng-yongsheng
 */
public class ServiceInstanceInventoryCacheDAO extends EsDAO implements IServiceInstanceInventoryCacheDAO {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceInventoryCacheDAO.class);

    protected final ServiceInstanceInventory.Builder builder = new ServiceInstanceInventory.Builder();
    protected final int resultWindowMaxSize;

    public ServiceInstanceInventoryCacheDAO(ElasticSearchClient client, int resultWindowMaxSize) {
        super(client);
        this.resultWindowMaxSize = resultWindowMaxSize;
    }

    @Override public ServiceInstanceInventory get(int serviceInstanceId) {
        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.termQuery(ServiceInstanceInventory.SEQUENCE, serviceInstanceId));
            searchSourceBuilder.size(1);

            SearchResponse response = getClient().search(ServiceInstanceInventory.INDEX_NAME, searchSourceBuilder);
            if (response.getHits().totalHits == 1) {
                SearchHit searchHit = response.getHits().getAt(0);
                return builder.map2Data(searchHit.getSourceAsMap());
            } else {
                return null;
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return null;
        }
    }

    @Override public int getServiceInstanceId(int serviceId, String uuid) {
        String id = ServiceInstanceInventory.buildId(serviceId, uuid);
        return get(id);
    }

    @Override public int getServiceInstanceId(int serviceId, int addressId) {
        String id = ServiceInstanceInventory.buildId(serviceId, addressId);
        return get(id);
    }

    @Override public List<ServiceInstanceInventory> loadLastUpdate(long lastUpdateTime) {
        List<ServiceInstanceInventory> instanceInventories = new ArrayList<>();

        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must().add(QueryBuilders.termQuery(ServiceInstanceInventory.IS_ADDRESS, BooleanUtils.TRUE));
            boolQuery.must().add(QueryBuilders.rangeQuery(ServiceInstanceInventory.LAST_UPDATE_TIME).gte(lastUpdateTime));

            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.size(resultWindowMaxSize);

            SearchResponse response = getClient().search(ServiceInstanceInventory.INDEX_NAME, searchSourceBuilder);

            for (SearchHit searchHit : response.getHits().getHits()) {
                instanceInventories.add(this.builder.map2Data(searchHit.getSourceAsMap()));
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        return instanceInventories;
    }

    private int get(String id) {
        try {
            GetResponse response = getClient().get(ServiceInstanceInventory.INDEX_NAME, id);
            if (response.isExists()) {
                return (int)response.getSource().getOrDefault(RegisterSource.SEQUENCE, 0);
            } else {
                return Const.NONE;
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return Const.NONE;
        }
    }
}
