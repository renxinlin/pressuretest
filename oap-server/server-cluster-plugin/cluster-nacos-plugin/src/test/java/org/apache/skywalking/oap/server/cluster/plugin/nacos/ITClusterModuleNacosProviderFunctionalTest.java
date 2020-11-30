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

package org.apache.skywalking.oap.server.cluster.plugin.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.cluster.ClusterNodesQuery;
import org.apache.skywalking.oap.server.core.cluster.ClusterRegister;
import org.apache.skywalking.oap.server.core.cluster.RemoteInstance;
import org.apache.skywalking.oap.server.core.remote.client.Address;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author zhangwei
 */
public class ITClusterModuleNacosProviderFunctionalTest {


    private String nacosAddress;

    @Before
    public void before() {
        nacosAddress = System.getProperty("nacos.address");
        assertFalse(StringUtil.isEmpty(nacosAddress));
    }

    @Test
    public void registerRemote() throws Exception {
        final String serviceName = "register_remote";
        ModuleProvider provider = createProvider(serviceName);

        Address selfAddress = new Address("127.0.0.1", 1000, true);
        RemoteInstance instance = new RemoteInstance(selfAddress);
        getClusterRegister(provider).registerRemote(instance);

        List<RemoteInstance> remoteInstances = queryRemoteNodes(provider, 1);
        assertEquals(1, remoteInstances.size());
        Address queryAddress = remoteInstances.get(0).getAddress();
        assertEquals(selfAddress, queryAddress);
        assertTrue(queryAddress.isSelf());
    }

    @Test
    public void registerRemoteOfReceiver() throws Exception {
        final String serviceName = "register_remote_receiver";
        ModuleProvider providerA = createProvider(serviceName);
        ModuleProvider providerB = createProvider(serviceName);

        // Mixed or Aggregator
        Address selfAddress = new Address("127.0.0.3", 1000, true);
        RemoteInstance instance = new RemoteInstance(selfAddress);
        getClusterRegister(providerA).registerRemote(instance);

        // Receiver
        List<RemoteInstance> remoteInstances = queryRemoteNodes(providerB, 1);
        assertEquals(1, remoteInstances.size());
        Address queryAddress = remoteInstances.get(0).getAddress();
        assertEquals(selfAddress, queryAddress);
        assertFalse(queryAddress.isSelf());
    }

    @Test
    public void registerRemoteOfCluster() throws Exception {
        final String serviceName = "register_remote_cluster";
        ModuleProvider providerA = createProvider(serviceName);
        ModuleProvider providerB = createProvider(serviceName);

        Address addressA = new Address("127.0.0.4", 1000, true);
        Address addressB = new Address("127.0.0.5", 1000, true);

        RemoteInstance instanceA = new RemoteInstance(addressA);
        RemoteInstance instanceB = new RemoteInstance(addressB);

        getClusterRegister(providerA).registerRemote(instanceA);
        getClusterRegister(providerB).registerRemote(instanceB);

        List<RemoteInstance> remoteInstancesOfA = queryRemoteNodes(providerA, 2);
        validateServiceInstance(addressA, addressB, remoteInstancesOfA);

        List<RemoteInstance> remoteInstancesOfB = queryRemoteNodes(providerB, 2);
        validateServiceInstance(addressB, addressA, remoteInstancesOfB);
    }

    @Test
    public void deregisterRemoteOfCluster() throws Exception {
        final String serviceName = "deregister_remote_cluster";
        ModuleProvider providerA = createProvider(serviceName);
        ModuleProvider providerB = createProvider(serviceName);

        Address addressA = new Address("127.0.0.6", 1000, true);
        Address addressB = new Address("127.0.0.7", 1000, true);

        RemoteInstance instanceA = new RemoteInstance(addressA);
        RemoteInstance instanceB = new RemoteInstance(addressB);

        getClusterRegister(providerA).registerRemote(instanceA);
        getClusterRegister(providerB).registerRemote(instanceB);

        List<RemoteInstance> remoteInstancesOfA = queryRemoteNodes(providerA, 2);
        validateServiceInstance(addressA, addressB, remoteInstancesOfA);

        List<RemoteInstance> remoteInstancesOfB = queryRemoteNodes(providerB, 2);
        validateServiceInstance(addressB, addressA, remoteInstancesOfB);

        // deregister A
        ClusterRegister register = getClusterRegister(providerA);
        NamingService namingServiceA = Whitebox.getInternalState(register, "namingService");
        namingServiceA.deregisterInstance(serviceName, addressA.getHost(), addressA.getPort());

        // only B
        remoteInstancesOfB = queryRemoteNodes(providerB, 1);
        assertEquals(1, remoteInstancesOfB.size());
        Address address = remoteInstancesOfB.get(0).getAddress();
        assertEquals(addressB, address);
        assertTrue(address.isSelf());
    }

    private ClusterModuleNacosProvider createProvider(String servicName) throws ModuleStartException {
        ClusterModuleNacosProvider provider = new ClusterModuleNacosProvider();

        ClusterModuleNacosConfig config = (ClusterModuleNacosConfig) provider.createConfigBeanIfAbsent();

        config.setHostPort(nacosAddress);
        config.setServiceName(servicName);

        provider.prepare();
        provider.start();
        provider.notifyAfterCompleted();
        return provider;
    }

    private ClusterRegister getClusterRegister(ModuleProvider provider) {
        return provider.getService(ClusterRegister.class);
    }

    private ClusterNodesQuery getClusterNodesQuery(ModuleProvider provider) {
        return provider.getService(ClusterNodesQuery.class);
    }

    private List<RemoteInstance> queryRemoteNodes(ModuleProvider provider, int goals) throws InterruptedException {
        int i = 20;
        do {
            List<RemoteInstance> instances = getClusterNodesQuery(provider).queryRemoteNodes();
            if (instances.size() == goals) {
                return instances;
            } else {
                Thread.sleep(1000);
            }
        } while (--i > 0);
        return Collections.EMPTY_LIST;
    }

    private void validateServiceInstance(Address selfAddress, Address otherAddress, List<RemoteInstance> queryResult) {
        assertEquals(2, queryResult.size());

        boolean selfExist = false, otherExist = false;

        for (RemoteInstance instance : queryResult) {
            Address queryAddress = instance.getAddress();
            if (queryAddress.equals(selfAddress) && queryAddress.isSelf()) {
                selfExist = true;
            } else if (queryAddress.equals(otherAddress) && !queryAddress.isSelf()) {
                otherExist = true;
            }
        }

        assertTrue(selfExist);
        assertTrue(otherExist);
    }

}
