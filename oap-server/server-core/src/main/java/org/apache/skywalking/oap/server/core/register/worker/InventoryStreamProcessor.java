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

package org.apache.skywalking.oap.server.core.register.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.UnexpectedException;
import org.apache.skywalking.oap.server.core.analysis.Downsampling;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.StreamProcessor;
import org.apache.skywalking.oap.server.core.register.RegisterSource;
import org.apache.skywalking.oap.server.core.storage.IRegisterDAO;
import org.apache.skywalking.oap.server.core.storage.StorageDAO;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.annotation.Storage;
import org.apache.skywalking.oap.server.core.storage.model.IModelSetter;
import org.apache.skywalking.oap.server.core.storage.model.Model;
import org.apache.skywalking.oap.server.core.worker.IWorkerInstanceSetter;
import org.apache.skywalking.oap.server.library.module.ModuleDefineHolder;

/**
 * @author peng-yongsheng
 */
public class InventoryStreamProcessor implements StreamProcessor<RegisterSource> {

    private static final InventoryStreamProcessor PROCESSOR = new InventoryStreamProcessor();
    // 每一个work仅处理一个步骤 组合后负责一个数据的处理
    private Map<Class<? extends RegisterSource>, RegisterDistinctWorker> entryWorkers = new HashMap<>();

    public static InventoryStreamProcessor getInstance() {
        return PROCESSOR;
    }

    /*
        完成流式处理
         1 distinctWorker  服务去重[agent多次重试 或者存在集群多实例]
         2 remoteWorker    通过remoteSenderService发送远程节点进行处理
         3 persistentWorker
     */
    public void in(RegisterSource registerSource) {
        entryWorkers.get(registerSource.getClass()).in(registerSource);
    }

    @SuppressWarnings("unchecked")
    public void create(ModuleDefineHolder moduleDefineHolder, Stream stream,
        Class<? extends RegisterSource> inventoryClass) {
        StorageDAO storageDAO = moduleDefineHolder.find(StorageModule.NAME).provider().getService(StorageDAO.class);
        IRegisterDAO registerDAO;
        try {
            registerDAO = storageDAO.newRegisterDao(stream.builder().newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UnexpectedException("Create " + stream.builder().getSimpleName() + " register DAO failure.", e);
        }

        IModelSetter modelSetter = moduleDefineHolder.find(CoreModule.NAME).provider().getService(IModelSetter.class);
        // 添加到StorageModels 之后installer会根据其包含的model以及init模式进行存储层的table创建[es,db等]
        Model model = modelSetter.putIfAbsent(inventoryClass, stream.scopeId(), new Storage(stream.name(), false, false, Downsampling.None), false);
        // 最后一个worker 对请求去重 例如多个服务实例一起进行应用名注册  此外服务注册失败会进行去重
        RegisterPersistentWorker persistentWorker = new RegisterPersistentWorker(moduleDefineHolder, model.getName(), registerDAO, stream.scopeId());

        String remoteReceiverWorkerName = stream.name() + "_rec";
        IWorkerInstanceSetter workerInstanceSetter = moduleDefineHolder.find(CoreModule.NAME).provider().getService(IWorkerInstanceSetter.class);
        workerInstanceSetter.put(remoteReceiverWorkerName, persistentWorker, inventoryClass);
        // 发往其他节点处理
        /*
        core:
          default:
            # Mixed: Receive agent data, Level 1 aggregate, Level 2 aggregate
            # Receiver: Receive agent data, Level 1 aggregate
            # Aggregator: Level 2 aggregate


         */
        RegisterRemoteWorker remoteWorker = new RegisterRemoteWorker(moduleDefineHolder, remoteReceiverWorkerName);
        // 第一个worker
        RegisterDistinctWorker distinctWorker = new RegisterDistinctWorker(moduleDefineHolder, remoteWorker);

        entryWorkers.put(inventoryClass, distinctWorker);
    }

    /**
     * @return all register sourceScopeId class types
     */
    public List<Class> getAllRegisterSources() {
        List allSources = new ArrayList<>();
        entryWorkers.keySet().forEach(allSources::add);
        return allSources;
    }
}
