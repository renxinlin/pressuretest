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

import java.io.IOException;
import org.apache.skywalking.oap.server.configuration.api.ConfigurationModule;
import org.apache.skywalking.oap.server.configuration.api.DynamicConfigurationService;
import org.apache.skywalking.oap.server.core.analysis.*;
import org.apache.skywalking.oap.server.core.analysis.metrics.ApdexMetrics;
import org.apache.skywalking.oap.server.core.analysis.worker.MetricsStreamProcessor;
import org.apache.skywalking.oap.server.core.analysis.worker.TopNStreamProcessor;
import org.apache.skywalking.oap.server.core.annotation.AnnotationScan;
import org.apache.skywalking.oap.server.core.cache.*;
import org.apache.skywalking.oap.server.core.cluster.*;
import org.apache.skywalking.oap.server.core.command.CommandService;
import org.apache.skywalking.oap.server.core.config.*;
import org.apache.skywalking.oap.server.core.oal.rt.*;
import org.apache.skywalking.oap.server.core.query.*;
import org.apache.skywalking.oap.server.core.register.service.*;
import org.apache.skywalking.oap.server.core.remote.*;
import org.apache.skywalking.oap.server.core.remote.client.*;
import org.apache.skywalking.oap.server.core.remote.health.HealthCheckServiceHandler;
import org.apache.skywalking.oap.server.core.server.*;
import org.apache.skywalking.oap.server.core.source.*;
import org.apache.skywalking.oap.server.core.storage.PersistenceTimer;
import org.apache.skywalking.oap.server.core.storage.model.*;
import org.apache.skywalking.oap.server.core.storage.ttl.DataTTLKeeperTimer;
import org.apache.skywalking.oap.server.core.worker.*;
import org.apache.skywalking.oap.server.library.module.*;
import org.apache.skywalking.oap.server.library.server.ServerException;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCServer;
import org.apache.skywalking.oap.server.library.server.jetty.JettyServer;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;

/**
 * @author peng-yongsheng
 */
public class CoreModuleProvider extends ModuleProvider {

    private final CoreModuleConfig moduleConfig;
    // grpc服务器
    private GRPCServer grpcServer;
    // jetty服务器
    private JettyServer jettyServer;
    // 集群内其他节点的管理工具
    private RemoteClientManager remoteClientManager;
    // 注解扫描
    private final AnnotationScan annotationScan;
    // 存储模型管理器
    private final StorageModels storageModels;
    // SourceReceiver实现类
    private final SourceReceiverImpl receiver;
    // oal 引擎
    private OALEngine oalEngine;
    // apdex阈值配置
    private ApdexThresholdConfig apdexThresholdConfig;

    public CoreModuleProvider() {
        super();
        this.moduleConfig = new CoreModuleConfig();
        this.annotationScan = new AnnotationScan();
        this.storageModels = new StorageModels();
        this.receiver = new SourceReceiverImpl();
    }

    @Override public String name() {
        return "default";
    }

    @Override public Class<? extends ModuleDefine> module() {
        return CoreModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return moduleConfig;
    }

    @Override public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        // 通过annotationScan.registerListener注册StreamAnnotationListener
        // start阶段annotationScan会扫描所有的@stream注解,在扫描完成后会通过notify调用StreamAnnotationListener的create方法完成StorageModels.models填充

        StreamAnnotationListener streamAnnotationListener = new StreamAnnotationListener(getManager());
        AnnotationScan scopeScan = new AnnotationScan();
        // 扫描ScopeDeclaration注解
        scopeScan.registerListener(new DefaultScopeDefine.Listener());
        try {
            // 扫描ScopeDeclaration.Listener指定的ScopeDeclaration注解
            // 调用notify通知Listener哪些class配置了该注解
            scopeScan.scan();
            // oal[观测分析语言]解析引擎
            oalEngine = OALEngineLoader.get();// observability analysis language
            oalEngine.setStreamListener(streamAnnotationListener);
            // 根据oal动态生成Dispatcher到receiver
            oalEngine.setDispatcherListener(receiver.getDispatcherManager());
            // 结合 oap-server/server-bootstrap/official_analysis.oal定义以及oap-server/oal-rt/code-templates目录freemarker模板文件
            // 生成dispatcher到receiver中
            // 配置环境变量SW_OAL_ENGINE_DEBUG=TRUE可查看生成源码
            // 源码目录/oap-server/server-core/target/oal-rt [包括度量Metrics 度量转换器 以及分发处理器]
            oalEngine.start(getClass().getClassLoader());
        } catch (Exception e) {
            throw new ModuleStartException(e.getMessage(), e);
        }

        AnnotationScan oalDisable = new AnnotationScan();
        // 扫描MultipleDisable注解
        oalDisable.registerListener(DisableRegister.INSTANCE);
        oalDisable.registerListener(new DisableRegister.SingleDisableScanListener());
        try {
            oalDisable.scan();
        } catch (IOException e) {
            throw new ModuleStartException(e.getMessage(), e);
        }


        // 创建GRPCServer 支持grpc协议 响应agent 【server-library模块library-server子模块提供GRPCServer serverlib是一个工具包 负责提供通信相关工具】
        grpcServer = new GRPCServer(moduleConfig.getGRPCHost(), moduleConfig.getGRPCPort());
        if (moduleConfig.getMaxConcurrentCallsPerConnection() > 0) {
            grpcServer.setMaxConcurrentCallsPerConnection(moduleConfig.getMaxConcurrentCallsPerConnection());
        }
        if (moduleConfig.getMaxMessageSize() > 0) {
            grpcServer.setMaxMessageSize(moduleConfig.getMaxMessageSize());
        }
        if (moduleConfig.getGRPCThreadPoolQueueSize() > 0) {
            grpcServer.setThreadPoolQueueSize(moduleConfig.getGRPCThreadPoolQueueSize());
        }
        if (moduleConfig.getGRPCThreadPoolSize() > 0) {
            grpcServer.setThreadPoolSize(moduleConfig.getGRPCThreadPoolSize());
        }
        grpcServer.initialize();
        // 创建jettyServer 支持http协议,响应Skywalking-UI
        jettyServer = new JettyServer(moduleConfig.getRestHost(), moduleConfig.getRestPort(), moduleConfig.getRestContextPath(), moduleConfig.getJettySelectors());
        jettyServer.initialize();
        this.registerServiceImplementation(ConfigService.class, new ConfigService(moduleConfig));
        this.registerServiceImplementation(DownsamplingConfigService.class, new DownsamplingConfigService(moduleConfig.getDownsampling()));
        // 注册GRPCHandlerRegister和JettyHandlerRegister,通过addHandler增加对不同请求的处理
        this.registerServiceImplementation(GRPCHandlerRegister.class, new GRPCHandlerRegisterImpl(grpcServer));
        this.registerServiceImplementation(JettyHandlerRegister.class, new JettyHandlerRegisterImpl(jettyServer));

        this.registerServiceImplementation(IComponentLibraryCatalogService.class, new ComponentLibraryCatalogService());
        // 注册receiver,可将server端接收到的agent数据进行分发处理
        this.registerServiceImplementation(SourceReceiver.class, receiver);

        WorkerInstancesService instancesService = new WorkerInstancesService();
        this.registerServiceImplementation(IWorkerInstanceGetter.class, instancesService);
        this.registerServiceImplementation(IWorkerInstanceSetter.class, instancesService);
        // server端集群其他节点通信工具
        this.registerServiceImplementation(RemoteSenderService.class, new RemoteSenderService(getManager()));
        // 模型管理容器[每一个模型映射es一个索引 类似mybatis的entity与db的table映射]
        this.registerServiceImplementation(IModelSetter.class, storageModels);
        this.registerServiceImplementation(IModelGetter.class, storageModels);
        this.registerServiceImplementation(IModelOverride.class, storageModels);

        // start  agent服务注册发现相关//////////////////////////////////////////////////////////////////////////////////////////////////////////
        this.registerServiceImplementation(ServiceInventoryCache.class, new ServiceInventoryCache(getManager(), moduleConfig));
        this.registerServiceImplementation(IServiceInventoryRegister.class, new ServiceInventoryRegister(getManager()));

        this.registerServiceImplementation(ServiceInstanceInventoryCache.class, new ServiceInstanceInventoryCache(getManager(), moduleConfig));
        this.registerServiceImplementation(IServiceInstanceInventoryRegister.class, new ServiceInstanceInventoryRegister(getManager()));

        // 将endpoint 和NetworkAddress等字符串信息注册到字典中 agent通过id
        this.registerServiceImplementation(EndpointInventoryCache.class, new EndpointInventoryCache(getManager(), moduleConfig));
        this.registerServiceImplementation(IEndpointInventoryRegister.class, new EndpointInventoryRegister(getManager()));
        //  查询网络地址  服务  服务实例
        this.registerServiceImplementation(NetworkAddressInventoryCache.class, new NetworkAddressInventoryCache(getManager(), moduleConfig));
        this.registerServiceImplementation(INetworkAddressInventoryRegister.class, new NetworkAddressInventoryRegister(getManager()));
        // end //////////////////////////////////////////////////////////////////////////////////////////////////////////

        // skywalking - ui面板查询相关
        this.registerServiceImplementation(TopologyQueryService.class, new TopologyQueryService(getManager()));
        this.registerServiceImplementation(MetricQueryService.class, new MetricQueryService(getManager()));
        this.registerServiceImplementation(TraceQueryService.class, new TraceQueryService(getManager()));
        this.registerServiceImplementation(LogQueryService.class, new LogQueryService(getManager()));
        this.registerServiceImplementation(MetadataQueryService.class, new MetadataQueryService(getManager()));
        this.registerServiceImplementation(AggregationQueryService.class, new AggregationQueryService(getManager()));
        this.registerServiceImplementation(AlarmQueryService.class, new AlarmQueryService(getManager()));
        this.registerServiceImplementation(TopNRecordsQueryService.class, new TopNRecordsQueryService(getManager()));

        this.registerServiceImplementation(CommandService.class, new CommandService(getManager()));
        // 处理@Stream注解
        annotationScan.registerListener(streamAnnotationListener);

        this.remoteClientManager = new RemoteClientManager(getManager(), moduleConfig.getRemoteTimeout());
        this.registerServiceImplementation(RemoteClientManager.class, remoteClientManager);
        // 流式处理器的一些配置
        MetricsStreamProcessor.getInstance().setEnableDatabaseSession(moduleConfig.isEnableDatabaseSession());
        TopNStreamProcessor.getInstance().setTopNWorkerReportCycle(moduleConfig.getTopNReportPeriod());
        apdexThresholdConfig = new ApdexThresholdConfig(this);
        ApdexMetrics.setDICT(apdexThresholdConfig);
    }

    @Override public void start() throws ModuleStartException {
        // 负责OAP集群节点之间的通信
        grpcServer.addHandler(new RemoteServiceHandler(getManager()));
        // 负责外部比如consul等对节点自身的服务健康检查
        grpcServer.addHandler(new HealthCheckServiceHandler());
        // 提供集群的读写能力,内部借助定时任务自动刷新集群信息
        remoteClientManager.start();

        try {
            // 除去oal引擎动态编译 这里自动扫描源码中的SourceDispatcher实现并获取其处理的Source泛型对象的scopeid
            // 构建scopeid到SourceDispatcher的映射
            receiver.scan();
            // 处理@Stream注解: 构建不同维度元祖数据的Processor stream流式worker的初始化 以及Model索引实体对象的构建
            annotationScan.scan();
            // 自动生成类也执行annotationScan.scan的底层逻辑 【stream流式worker的初始化 模型初始化】
            oalEngine.notifyAllListeners();
        } catch (IOException | IllegalAccessException | InstantiationException e) {
            throw new ModuleStartException(e.getMessage(), e);
        }

        // 集群角色为Mixed和Aggregator则参与集群注册,Receiver不参与集群注册
        if (CoreModuleConfig.Role.Mixed.name().equalsIgnoreCase(moduleConfig.getRole()) || CoreModuleConfig.Role.Aggregator.name().equalsIgnoreCase(moduleConfig.getRole())) {
            RemoteInstance gRPCServerInstance = new RemoteInstance(new Address(moduleConfig.getGRPCHost(), moduleConfig.getGRPCPort(), true));
            // mixed和Aggregator两种角色的oapserver节点会加入集群【默认是mixed和Aggregator】
            //
            this.getManager().find(ClusterModule.NAME).provider().getService(ClusterRegister.class).registerRemote(gRPCServerInstance);
        }
        // 注册一个配置 apdexThresholdConfig
        DynamicConfigurationService dynamicConfigurationService = getManager().find(ConfigurationModule.NAME).provider().getService(DynamicConfigurationService.class);
        dynamicConfigurationService.registerConfigChangeWatcher(apdexThresholdConfig);
    }

    @Override public void notifyAfterCompleted() throws ModuleStartException {
        // 启动grpc服务器和jetty服务器 开始正式对外提供工作
        try {
            grpcServer.start();
            jettyServer.start();
        } catch (ServerException e) {
            throw new ModuleStartException(e.getMessage(), e);
        }
        // 每5秒持久化数据
        PersistenceTimer.INSTANCE.start(getManager(), moduleConfig);
        // skywalking采集的数据量很大,对能够清理的数据集进行清理
        if (moduleConfig.isEnableDataKeeperExecutor()) {
            DataTTLKeeperTimer.INSTANCE.start(getManager(), moduleConfig);
        }
        // 缓存更新
        CacheUpdateTimer.INSTANCE.start(getManager());
    }

    @Override
    public String[] requiredModules() {
        return new String[] {TelemetryModule.NAME, ConfigurationModule.NAME};
    }
}
