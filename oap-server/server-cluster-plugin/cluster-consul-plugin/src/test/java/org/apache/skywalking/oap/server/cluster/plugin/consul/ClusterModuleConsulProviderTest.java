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
package org.apache.skywalking.oap.server.cluster.plugin.consul;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.cluster.ClusterModule;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by dengming, 2019.05.01
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Consul.class)
@PowerMockIgnore("javax.management.*")
public class ClusterModuleConsulProviderTest {

    private ClusterModuleConsulProvider provider = new ClusterModuleConsulProvider();

    @Test
    public void name() {
        assertEquals("consul", provider.name());
    }

    @Test
    public void module() {
        assertEquals(ClusterModule.class, provider.module());
    }

    @Test
    public void createConfigBeanIfAbsent() {
        ModuleConfig moduleConfig = provider.createConfigBeanIfAbsent();
        assertTrue(moduleConfig instanceof ClusterModuleConsulConfig);
    }

    @Test(expected = ModuleStartException.class)
    public void prepareWithNonHost() throws Exception {
        provider.prepare();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prepare() throws Exception {
        ClusterModuleConsulConfig consulConfig = new ClusterModuleConsulConfig();
        consulConfig.setHostPort("10.0.0.1:1000,10.0.0.2:1001");
        Whitebox.setInternalState(provider, "config", consulConfig);

        Consul consulClient = mock(Consul.class);
        Consul.Builder builder = mock(Consul.Builder.class);
        when(builder.build()).thenReturn(consulClient);

        PowerMockito.mockStatic(Consul.class);
        when(Consul.builder()).thenReturn(builder);
        when(builder.withConnectTimeoutMillis(anyLong())).thenReturn(builder);

        when(builder.withMultipleHostAndPort(anyCollection(), anyLong())).thenReturn(builder);
        provider.prepare();

        ArgumentCaptor<Collection> addressCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(long.class);
        verify(builder).withMultipleHostAndPort(addressCaptor.capture(), timeCaptor.capture());

        List<HostAndPort> address = (List<HostAndPort>) addressCaptor.getValue();
        assertEquals(2, address.size());
        assertEquals(Lists.newArrayList(HostAndPort.fromParts("10.0.0.1", 1000),
                HostAndPort.fromParts("10.0.0.2", 1001)
        ), address);
    }

    @Test
    public void prepareSingle() throws Exception {
        ClusterModuleConsulConfig consulConfig = new ClusterModuleConsulConfig();
        consulConfig.setHostPort("10.0.0.1:1000");
        Whitebox.setInternalState(provider, "config", consulConfig);

        Consul consulClient = mock(Consul.class);
        Consul.Builder builder = mock(Consul.Builder.class);
        when(builder.build()).thenReturn(consulClient);

        PowerMockito.mockStatic(Consul.class);
        when(Consul.builder()).thenReturn(builder);
        when(builder.withConnectTimeoutMillis(anyLong())).thenCallRealMethod();

        when(builder.withHostAndPort(any())).thenReturn(builder);

        provider.prepare();

        ArgumentCaptor<HostAndPort> hostAndPortArgumentCaptor = ArgumentCaptor.forClass(HostAndPort.class);
        verify(builder).withHostAndPort(hostAndPortArgumentCaptor.capture());

        HostAndPort address = hostAndPortArgumentCaptor.getValue();
        assertEquals(HostAndPort.fromParts("10.0.0.1", 1000), address);
    }

    @Test
    public void start() {
        provider.start();
    }

    @Test
    public void notifyAfterCompleted() {
        provider.notifyAfterCompleted();
    }

    @Test
    public void requiredModules() {
        String[] modules = provider.requiredModules();
        assertArrayEquals(new String[]{CoreModule.NAME}, modules);
    }
}