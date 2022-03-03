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

package org.apache.skywalking.oap.server.core;

import java.util.ArrayList;
import java.util.List;
import org.apache.skywalking.oap.server.core.cache.EndpointInventoryCache;
import org.apache.skywalking.oap.server.core.cache.NetworkAddressInventoryCache;
import org.apache.skywalking.oap.server.core.cache.ServiceInstanceInventoryCache;
import org.apache.skywalking.oap.server.core.cache.ServiceInventoryCache;
import org.apache.skywalking.oap.server.core.command.CommandService;
import org.apache.skywalking.oap.server.core.config.ConfigService;
import org.apache.skywalking.oap.server.core.config.DownsamplingConfigService;
import org.apache.skywalking.oap.server.core.config.IComponentLibraryCatalogService;
import org.apache.skywalking.oap.server.core.query.AggregationQueryService;
import org.apache.skywalking.oap.server.core.query.AlarmQueryService;
import org.apache.skywalking.oap.server.core.query.LogQueryService;
import org.apache.skywalking.oap.server.core.query.MetadataQueryService;
import org.apache.skywalking.oap.server.core.query.MetricQueryService;
import org.apache.skywalking.oap.server.core.query.TopNRecordsQueryService;
import org.apache.skywalking.oap.server.core.query.TopologyQueryService;
import org.apache.skywalking.oap.server.core.query.TraceQueryService;
import org.apache.skywalking.oap.server.core.register.service.IEndpointInventoryRegister;
import org.apache.skywalking.oap.server.core.register.service.INetworkAddressInventoryRegister;
import org.apache.skywalking.oap.server.core.register.service.IServiceInstanceInventoryRegister;
import org.apache.skywalking.oap.server.core.register.service.IServiceInventoryRegister;
import org.apache.skywalking.oap.server.core.remote.RemoteSenderService;
import org.apache.skywalking.oap.server.core.remote.client.RemoteClientManager;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.core.server.JettyHandlerRegister;
import org.apache.skywalking.oap.server.core.source.SourceReceiver;
import org.apache.skywalking.oap.server.core.storage.model.IModelGetter;
import org.apache.skywalking.oap.server.core.storage.model.IModelOverride;
import org.apache.skywalking.oap.server.core.storage.model.IModelSetter;
import org.apache.skywalking.oap.server.core.worker.IWorkerInstanceGetter;
import org.apache.skywalking.oap.server.core.worker.IWorkerInstanceSetter;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;

/**
 * @author peng-yongsheng
 */
public class CoreModule extends ModuleDefine {

    public static final String NAME = "core";

    public CoreModule() {
        super(NAME);
    }

    @Override public Class[] services() {
        List<Class> classes = new ArrayList<>();
        // 配置grpc服务ip信息,数据清理ttl时间
        classes.add(ConfigService.class);
        // 降采样,通过减少数据采集,降低存储压力
        classes.add(DownsamplingConfigService.class);
        // 加载component-libraries.yml
        classes.add(IComponentLibraryCatalogService.class);
        // 远程工作实例获取类
        classes.add(IWorkerInstanceGetter.class);
        // 远程工作实例设置类
        classes.add(IWorkerInstanceSetter.class);
        // 添加core模块的服务器
        addServerInterface(classes);
        // 添加数据上报的处理服务SourceReceiver
        addReceiverInterface(classes);
        // 模型管理以及集群内部通信
        addInsideService(classes);
        // 添加服务注册相关的service
        addRegisterService(classes);
        // 添加缓存相关
        addCacheService(classes);
        // 添加ui查询相关service
        addQueryService(classes);
        classes.add(CommandService.class);
        return classes.toArray(new Class[] {});
    }

    // skywalking-ui通过这些查询于网页展示数据
    private void addQueryService(List<Class> classes) {
        // 拓扑图查询
        classes.add(TopologyQueryService.class);
        // 度量查询
        classes.add(MetricQueryService.class);
        // 链路查询
        classes.add(TraceQueryService.class);
        // 日志查询
        classes.add(LogQueryService.class);
        // 元数据查询
        classes.add(MetadataQueryService.class);
        // 聚合查询
        classes.add(AggregationQueryService.class);
        // 告警查询
        classes.add(AlarmQueryService.class);
        // topN查询
        classes.add(TopNRecordsQueryService.class);
    }

    private void addServerInterface(List<Class> classes) {
        /*
            核心模块: GRPCHandlerRegisterImpl
            共享服务模块: ReceiverGRPCHandlerRegister,通过addHandler添加业务场景
         */
        // 负责grpc协议处理
        classes.add(GRPCHandlerRegister.class);
        /*
            核心模块: JettyHandlerRegisterImpl
            共享服务模块: ReceiverJettyHandlerRegister
         */
        // 负责http协议处理,通过addHandler添加业务场景
        classes.add(JettyHandlerRegister.class);
    }

    private void addInsideService(List<Class> classes) {
        ////////////////////////////////////////////StorageModels 实现start////////////////////////////////////////////////////////////
        classes.add(IModelSetter.class);
        classes.add(IModelGetter.class);
        classes.add(IModelOverride.class);
        ////////////////////////////////////////////StorageModels 实现end////////////////////////////////////////////////////////////
        classes.add(RemoteClientManager.class);
        classes.add(RemoteSenderService.class);
    }

    private void addRegisterService(List<Class> classes) {
        // 注册服务 负责agent的服务注册 服务实例注册  endpoint和network同步信息上报
        classes.add(IServiceInventoryRegister.class);
        classes.add(IServiceInstanceInventoryRegister.class);
        classes.add(IEndpointInventoryRegister.class);
        classes.add(INetworkAddressInventoryRegister.class);
    }

    private void addCacheService(List<Class> classes) {
        // 缓存注册信息,拉取时不再走es 而是走缓存,缓存不存在才走es
        classes.add(ServiceInventoryCache.class);
        classes.add(ServiceInstanceInventoryCache.class);
        classes.add(EndpointInventoryCache.class);
        classes.add(NetworkAddressInventoryCache.class);
    }

    private void addReceiverInterface(List<Class> classes) {
        // SourceReceiver底层依赖存储模块进行存储 但其Provider的requiredModules只检查了TelemetryModule.NAME和ConfigurationModule.NAME
        // 也就说模块与模块之间的依赖requiredModules只是一种约束,并非真实依赖关系
        // 同时不同的模块之间应该尽量避免耦合,来保障插件的可插拔
        // 插件之间的依赖关系尽量保障成树状关系,避免网状关系

        // 接收数据进行分发,交由分发器处理[聚合+存储]
        classes.add(SourceReceiver.class);
    }
}
