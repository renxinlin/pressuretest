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

import java.io.*;

import org.apache.skywalking.oap.server.configuration.api.ConfigurationModule;
import org.apache.skywalking.oap.server.configuration.api.DynamicConfigurationService;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.alarm.*;
import org.apache.skywalking.oap.server.library.module.*;
import org.apache.skywalking.oap.server.library.util.ResourceUtils;

public class AlarmModuleProvider extends ModuleProvider {

    private NotifyHandler notifyHandler;
    private AlarmRulesWatcher alarmRulesWatcher;

    @Override public String name() {
        return "default";
    }

    @Override public Class<? extends ModuleDefine> module() {
        return AlarmModule.class;
    }

    @Override public ModuleConfig createConfigBeanIfAbsent() {
        return new AlarmSettings();
    }

    @Override public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        Reader applicationReader;
        try {
            // 读取告警规则配置
            applicationReader = ResourceUtils.read("alarm-settings.yml");
        } catch (FileNotFoundException e) {
            throw new ModuleStartException("can't load alarm-settings.yml", e);
        }
        RulesReader reader = new RulesReader(applicationReader);
        // 包含告警规则AlarmRule以及 告警通知地址webhooks【这里通知比较简单,生产中可以支持短信,电话,工作群,邮件,第三方软件等】
        Rules rules = reader.readRules();
        // 监视数据 进行告警
        alarmRulesWatcher = new AlarmRulesWatcher(rules, this);
        // 数据进入告警模块处理的入口
        notifyHandler = new NotifyHandler(alarmRulesWatcher);
        // 添加告警消息的持久化回调 以及启动异步线程开始工作
        notifyHandler.init(new AlarmStandardPersistence());
        this.registerServiceImplementation(MetricsNotify.class, notifyHandler);
    }

    @Override public void start() throws ServiceNotProvidedException, ModuleStartException {
        DynamicConfigurationService dynamicConfigurationService = getManager().find(ConfigurationModule.NAME).provider().getService(DynamicConfigurationService.class);
        // 注册alarmRulesWatcher到配置中心,alarmRulesWatcher.notify可以看出其支持告警规则的动态修改
        dynamicConfigurationService.registerConfigChangeWatcher(alarmRulesWatcher);
    }

    @Override public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {
        notifyHandler.initCache(getManager());
    }

    @Override public String[] requiredModules() {
        return new String[] {CoreModule.NAME, ConfigurationModule.NAME};
    }
}
