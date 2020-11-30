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

package org.apache.skywalking.oap.server.core.analysis.worker;

import java.util.*;
import lombok.*;
import org.apache.skywalking.oap.server.core.*;
import org.apache.skywalking.oap.server.core.analysis.*;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.core.config.DownsamplingConfigService;
import org.apache.skywalking.oap.server.core.storage.*;
import org.apache.skywalking.oap.server.core.storage.annotation.Storage;
import org.apache.skywalking.oap.server.core.storage.model.*;
import org.apache.skywalking.oap.server.core.worker.IWorkerInstanceSetter;
import org.apache.skywalking.oap.server.library.module.ModuleDefineHolder;

/**
 * @author peng-yongsheng
 */
public class MetricsStreamProcessor implements StreamProcessor<Metrics> {

    private final static MetricsStreamProcessor PROCESSOR = new MetricsStreamProcessor();

    private Map<Class<? extends Metrics>, MetricsAggregateWorker> entryWorkers = new HashMap<>();
    @Getter private List<MetricsPersistentWorker> persistentWorkers = new ArrayList<>();
    @Setter @Getter private boolean enableDatabaseSession;

    public static MetricsStreamProcessor getInstance() {
        return PROCESSOR;
    }

    public void in(Metrics metrics) {
        MetricsAggregateWorker worker = entryWorkers.get(metrics.getClass());
        if (worker != null) {
            worker.in(metrics);
        }
    }

    @SuppressWarnings("unchecked")
    public void create(ModuleDefineHolder moduleDefineHolder, Stream stream, Class<? extends Metrics> metricsClass) {
        if (DisableRegister.INSTANCE.include(stream.name())) {
            return;
        }

        StorageDAO storageDAO = moduleDefineHolder.find(StorageModule.NAME).provider().getService(StorageDAO.class);
        IMetricsDAO metricsDAO;
        try {
            metricsDAO = storageDAO.newMetricsDao(stream.builder().newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UnexpectedException("Create " + stream.builder().getSimpleName() + " metrics DAO failure.", e);
        }

        IModelSetter modelSetter = moduleDefineHolder.find(CoreModule.NAME).provider().getService(IModelSetter.class);
        DownsamplingConfigService configService = moduleDefineHolder.find(CoreModule.NAME).provider().getService(DownsamplingConfigService.class);

        MetricsPersistentWorker hourPersistentWorker = null;
        MetricsPersistentWorker dayPersistentWorker = null;
        MetricsPersistentWorker monthPersistentWorker = null;

        if (configService.shouldToHour()) {
            Model model = modelSetter.putIfAbsent(metricsClass, stream.scopeId(), new Storage(stream.name(), true, true, Downsampling.Hour), false);
            hourPersistentWorker = worker(moduleDefineHolder, metricsDAO, model);
        }
        if (configService.shouldToDay()) {
            Model model = modelSetter.putIfAbsent(metricsClass, stream.scopeId(), new Storage(stream.name(), true, true, Downsampling.Day), false);
            dayPersistentWorker = worker(moduleDefineHolder, metricsDAO, model);
        }
        if (configService.shouldToMonth()) {
            Model model = modelSetter.putIfAbsent(metricsClass, stream.scopeId(), new Storage(stream.name(), true, true, Downsampling.Month), false);
            monthPersistentWorker = worker(moduleDefineHolder, metricsDAO, model);
        }

        MetricsTransWorker transWorker = new MetricsTransWorker(moduleDefineHolder, stream.name(), hourPersistentWorker, dayPersistentWorker, monthPersistentWorker);

        Model model = modelSetter.putIfAbsent(metricsClass, stream.scopeId(), new Storage(stream.name(), true, true, Downsampling.Minute), false);
        MetricsPersistentWorker minutePersistentWorker = minutePersistentWorker(moduleDefineHolder, metricsDAO, model, transWorker);

        String remoteReceiverWorkerName = stream.name() + "_rec";
        IWorkerInstanceSetter workerInstanceSetter = moduleDefineHolder.find(CoreModule.NAME).provider().getService(IWorkerInstanceSetter.class);
        workerInstanceSetter.put(remoteReceiverWorkerName, minutePersistentWorker, metricsClass);

        MetricsRemoteWorker remoteWorker = new MetricsRemoteWorker(moduleDefineHolder, remoteReceiverWorkerName);
        MetricsAggregateWorker aggregateWorker = new MetricsAggregateWorker(moduleDefineHolder, remoteWorker, stream.name());

        entryWorkers.put(metricsClass, aggregateWorker);
    }

    private MetricsPersistentWorker minutePersistentWorker(ModuleDefineHolder moduleDefineHolder, IMetricsDAO metricsDAO, Model model, MetricsTransWorker transWorker) {
        AlarmNotifyWorker alarmNotifyWorker = new AlarmNotifyWorker(moduleDefineHolder);
        ExportWorker exportWorker = new ExportWorker(moduleDefineHolder);

        MetricsPersistentWorker minutePersistentWorker = new MetricsPersistentWorker(moduleDefineHolder, model, metricsDAO, alarmNotifyWorker, exportWorker, transWorker, enableDatabaseSession);
        persistentWorkers.add(minutePersistentWorker);

        return minutePersistentWorker;
    }

    private MetricsPersistentWorker worker(ModuleDefineHolder moduleDefineHolder, IMetricsDAO metricsDAO, Model model) {
        MetricsPersistentWorker persistentWorker = new MetricsPersistentWorker(moduleDefineHolder, model, metricsDAO, null, null, null, enableDatabaseSession);
        persistentWorkers.add(persistentWorker);

        return persistentWorker;
    }
}
