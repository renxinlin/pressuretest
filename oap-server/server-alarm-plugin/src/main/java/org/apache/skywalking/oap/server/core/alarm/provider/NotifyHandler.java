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

package org.apache.skywalking.oap.server.core.alarm.provider;

import java.util.*;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.alarm.*;
import org.apache.skywalking.oap.server.core.analysis.metrics.*;
import org.apache.skywalking.oap.server.core.cache.*;
import org.apache.skywalking.oap.server.core.register.*;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

public class NotifyHandler implements MetricsNotify {
    private ServiceInventoryCache serviceInventoryCache;
    private ServiceInstanceInventoryCache serviceInstanceInventoryCache;
    private EndpointInventoryCache endpointInventoryCache;

    private final AlarmCore core;
    private final AlarmRulesWatcher alarmRulesWatcher;

    public NotifyHandler(AlarmRulesWatcher alarmRulesWatcher) {
        this.alarmRulesWatcher = alarmRulesWatcher;
        core = new AlarmCore(alarmRulesWatcher);
    }

    @Override public void notify(Metrics metrics) {
        WithMetadata withMetadata = (WithMetadata)metrics;
        MetricsMetaInfo meta = withMetadata.getMeta();
        int scope = meta.getScope();

        if (!DefaultScopeDefine.inServiceCatalog(scope)
            && !DefaultScopeDefine.inServiceInstanceCatalog(scope)
            && !DefaultScopeDefine.inEndpointCatalog(scope)) {
            return;
        }

        MetaInAlarm metaInAlarm;
        if (DefaultScopeDefine.inServiceCatalog(scope)) {
            int serviceId = Integer.parseInt(meta.getId());
            ServiceInventory serviceInventory = serviceInventoryCache.get(serviceId);
            ServiceMetaInAlarm serviceMetaInAlarm = new ServiceMetaInAlarm();
            serviceMetaInAlarm.setMetricsName(meta.getMetricsName());
            serviceMetaInAlarm.setId(serviceId);
            serviceMetaInAlarm.setName(serviceInventory.getName());
            metaInAlarm = serviceMetaInAlarm;
        } else if (DefaultScopeDefine.inServiceInstanceCatalog(scope)) {
            int serviceInstanceId = Integer.parseInt(meta.getId());
            ServiceInstanceInventory serviceInstanceInventory = serviceInstanceInventoryCache.get(serviceInstanceId);
            ServiceInstanceMetaInAlarm instanceMetaInAlarm = new ServiceInstanceMetaInAlarm();
            instanceMetaInAlarm.setMetricsName(meta.getMetricsName());
            instanceMetaInAlarm.setId(serviceInstanceId);
            instanceMetaInAlarm.setName(serviceInstanceInventory.getName());
            metaInAlarm = instanceMetaInAlarm;
        } else if (DefaultScopeDefine.inEndpointCatalog(scope)) {
            int endpointId = Integer.parseInt(meta.getId());
            EndpointInventory endpointInventory = endpointInventoryCache.get(endpointId);
            EndpointMetaInAlarm endpointMetaInAlarm = new EndpointMetaInAlarm();
            endpointMetaInAlarm.setMetricsName(meta.getMetricsName());
            endpointMetaInAlarm.setId(endpointId);

            int serviceId = endpointInventory.getServiceId();
            ServiceInventory serviceInventory = serviceInventoryCache.get(serviceId);

            String textName = endpointInventory.getName() + " in " + serviceInventory.getName();

            endpointMetaInAlarm.setName(textName);
            metaInAlarm = endpointMetaInAlarm;
        } else {
            return;
        }

        List<RunningRule> runningRules = core.findRunningRule(meta.getMetricsName());
        if (runningRules == null) {
            return;
        }

        runningRules.forEach(rule -> rule.in(metaInAlarm, metrics));
    }

    public void init(AlarmCallback... callbacks) {
        List<AlarmCallback> allCallbacks = new ArrayList<>(Arrays.asList(callbacks));
        allCallbacks.add(new WebhookCallback(alarmRulesWatcher));
        core.start(allCallbacks);
    }

    public void initCache(ModuleManager moduleManager) {
        serviceInventoryCache = moduleManager.find(CoreModule.NAME).provider().getService(ServiceInventoryCache.class);
        serviceInstanceInventoryCache = moduleManager.find(CoreModule.NAME).provider().getService(ServiceInstanceInventoryCache.class);
        endpointInventoryCache = moduleManager.find(CoreModule.NAME).provider().getService(EndpointInventoryCache.class);
    }
}
