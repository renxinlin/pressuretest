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

package org.apache.skywalking.oap.server.cluster.plugin.kubernetes;

import org.apache.skywalking.oap.server.cluster.plugin.kubernetes.dependencies.*;
import org.apache.skywalking.oap.server.core.cluster.*;
import org.apache.skywalking.oap.server.library.module.*;

/**
 * Use kubernetes to manage all instances in Skywalking cluster.
 *
 * @author gaohongtao
 */
public class ClusterModuleKubernetesProvider extends ModuleProvider {

    private final ClusterModuleKubernetesConfig config;
    private KubernetesCoordinator coordinator;

    public ClusterModuleKubernetesProvider() {
        super();
        this.config = new ClusterModuleKubernetesConfig();
    }

    @Override public String name() {
        return "kubernetes";
    }

    @Override public Class<? extends ModuleDefine> module() {
        return ClusterModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return config;
    }

    @Override public void prepare() throws ServiceNotProvidedException {
        coordinator = new KubernetesCoordinator(getManager(),
            new NamespacedPodListWatch(config.getNamespace(), config.getLabelSelector(), config.getWatchTimeoutSeconds()),
            new UidEnvSupplier(config.getUidEnvName()));
        this.registerServiceImplementation(ClusterRegister.class, coordinator);
        this.registerServiceImplementation(ClusterNodesQuery.class, coordinator);
    }

    @Override public void start() {

    }

    @Override public void notifyAfterCompleted() {
        coordinator.start();
    }

    @Override public String[] requiredModules() {
        return new String[0];
    }
}
