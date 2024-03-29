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

package org.apache.skywalking.oap.server.cluster.plugin.zookeeper;

import com.google.common.collect.Lists;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.cluster.ClusterModule;
import org.apache.skywalking.oap.server.core.cluster.ClusterNodesQuery;
import org.apache.skywalking.oap.server.core.cluster.ClusterRegister;
import org.apache.skywalking.oap.server.core.cluster.RemoteInstance;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Use Zookeeper to manage all instances in SkyWalking cluster.
 *
 * @author peng-yongsheng, Wu Sheng
 */
public class ClusterModuleZookeeperProvider extends ModuleProvider {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModuleZookeeperProvider.class);

    private static final String BASE_PATH = "/skywalking";

    private final ClusterModuleZookeeperConfig config;
    private CuratorFramework client;
    private ServiceDiscovery<RemoteInstance> serviceDiscovery;

    public ClusterModuleZookeeperProvider() {
        super();
        this.config = new ClusterModuleZookeeperConfig();
    }

    @Override public String name() {
        return "zookeeper";
    }

    @Override public Class module() {
        return ClusterModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return config;
    }

    @Override public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(config.getBaseSleepTimeMs(), config.getMaxRetries());
        // 构建zk CuratorFrameworkFactory
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .retryPolicy(retryPolicy)
            .connectString(config.getHostPort());
        // 访问控制安全相关
        if (config.isEnableACL()) {
            String authInfo = config.getExpression();
            if ("digest".equals(config.getSchema())) {
                try {
                    authInfo = DigestAuthenticationProvider.generateDigest(authInfo);
                } catch (NoSuchAlgorithmException e) {
                    throw new ModuleStartException(e.getMessage(), e);
                }
            } else {
                throw new ModuleStartException("Support digest schema only.");
            }
            final List<ACL> acls = Lists.newArrayList();
            acls.add(new ACL(ZooDefs.Perms.ALL, new Id(config.getSchema(), authInfo)));
            acls.add(new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE));

            ACLProvider provider = new ACLProvider() {
                @Override
                public List<ACL> getDefaultAcl() {
                    return acls;
                }

                @Override
                public List<ACL> getAclForPath(String s) {
                    return acls;
                }
            };
            builder.aclProvider(provider);
            builder.authorization(config.getSchema(), config.getExpression().getBytes());
        }
        // zkClient 构建
        client = builder.build();

        String path = BASE_PATH + (StringUtil.isEmpty(config.getNameSpace()) ? "" : "/" + config.getNameSpace());


        // zk curator-x-discovery模块,该模块支持封装了zk的服务注册发现能力
        serviceDiscovery = ServiceDiscoveryBuilder.builder(RemoteInstance.class).client(client)
            .basePath(path)
            .watchInstances(true)
                // SWInstanceSerializer 将数据序列化成json
            .serializer(new SWInstanceSerializer()).build();

        // 提供zk节点注册和集群信息读取
        ZookeeperCoordinator coordinator;
        try {
            client.start();
            // 阻塞下 等待连接成功
            client.blockUntilConnected();
            serviceDiscovery.start();
            // 构建服务注册发现对象封装zk 提供服务注册和服务发现能力
            coordinator = new ZookeeperCoordinator(config, serviceDiscovery);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ModuleStartException(e.getMessage(), e);
        }
        // 通过coordinator对集群的信息进行注册,注册数据维护在zk
        this.registerServiceImplementation(ClusterRegister.class, coordinator);
        // 通过coordinator对集群的信息进行查询
        this.registerServiceImplementation(ClusterNodesQuery.class, coordinator);
    }

    @Override public void start() {
    }

    @Override public void notifyAfterCompleted() {
    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }
}
