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

package org.apache.skywalking.oap.query.graphql.resolver;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import java.io.IOException;
import java.text.ParseException;
import org.apache.skywalking.oap.query.graphql.type.*;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.query.*;
import org.apache.skywalking.oap.server.core.query.entity.*;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

/**
 * @author peng-yongsheng
 */
public class MetricQuery implements GraphQLQueryResolver {

    private final ModuleManager moduleManager;
    private MetricQueryService metricQueryService;

    public MetricQuery(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private MetricQueryService getMetricQueryService() {
        if (metricQueryService == null) {
            this.metricQueryService = moduleManager.find(CoreModule.NAME).provider().getService(MetricQueryService.class);
        }
        return metricQueryService;
    }

    public IntValues getValues(final BatchMetricConditions metrics, final Duration duration) throws IOException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());

        return getMetricQueryService().getValues(metrics.getName(), metrics.getIds(), StepToDownsampling.transform(duration.getStep()), startTimeBucket, endTimeBucket);
    }

    public IntValues getLinearIntValues(final MetricCondition metrics, final Duration duration) throws IOException, ParseException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());

        return getMetricQueryService().getLinearIntValues(metrics.getName(), metrics.getId(), StepToDownsampling.transform(duration.getStep()), startTimeBucket, endTimeBucket);
    }

    public Thermodynamic getThermodynamic(final MetricCondition metrics, final Duration duration) throws IOException, ParseException {
        long startTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getStart());
        long endTimeBucket = DurationUtils.INSTANCE.exchangeToTimeBucket(duration.getEnd());

        return getMetricQueryService().getThermodynamic(metrics.getName(), metrics.getId(), StepToDownsampling.transform(duration.getStep()), startTimeBucket, endTimeBucket);
    }
}
