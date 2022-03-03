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

import lombok.Getter;
import org.apache.skywalking.oap.server.configuration.api.ConfigChangeWatcher;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.alarm.AlarmModule;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 *
 *           规则
 *   数据  ----check----通知
 * Alarm rules' settings can be dynamically updated via configuration center(s),
 * this class is responsible for monitoring the configuration and parsing them
 * into {@link Rules} and {@link #runningContext}.
 *
 * @author kezhenxu94
 * @since 6.5.0
 */
public class AlarmRulesWatcher extends ConfigChangeWatcher {
    @Getter
    private volatile Map<String /*指标名称*/, List<RunningRule> /* 一个指标可能会有多个规则配置 */> runningContext;
    private volatile Map<AlarmRule/* 静态配置的告警规则 */, RunningRule/* 除去静态的配置 含包括运行时指标上报时的窗口数据 */> alarmRuleRunningRuleMap;
    private volatile Rules rules;
    private volatile String settingsString;

    public AlarmRulesWatcher(Rules defaultRules, ModuleProvider provider) {
        super(AlarmModule.NAME, provider, "alarm-settings");
        this.runningContext = new HashMap<>();
        this.alarmRuleRunningRuleMap = new HashMap<>();
        this.settingsString = Const.EMPTY_STRING;

        notify(defaultRules);
    }

    @Override
    public void notify(ConfigChangeEvent value) {
        if (value.getEventType() == EventType.DELETE) {
            settingsString = Const.EMPTY_STRING;
            notify(new Rules());
        } else {
            settingsString = value.getNewValue();
            RulesReader rulesReader = new RulesReader(new StringReader(value.getNewValue()));
            Rules rules = rulesReader.readRules();
            notify(rules);
        }
    }

    /*
        读取alarm-settings.yml会触发规则信息的重载
        配置中心变化也会触发规则信息的重载
     */
    void notify(Rules newRules) {
        Map<AlarmRule, RunningRule> newAlarmRuleRunningRuleMap = new HashMap<>();
        Map<String, List<RunningRule>> newRunningContext = new HashMap<>();

        newRules.getRules().forEach(rule -> {
            /*
             * If there is already an alarm rule that is the same as the new one, we'll reuse its
             * corresponding runningRule, to keep its history metrics
             */
            RunningRule runningRule = alarmRuleRunningRuleMap.getOrDefault(rule, new RunningRule(rule));
            // 每一个AlarmRule对应一个RunningRule
            newAlarmRuleRunningRuleMap.put(rule, runningRule);
            // 多个AlarmRule 可能有同一个metricsName
            String metricsName = rule.getMetricsName();

            List<RunningRule> runningRules = newRunningContext.computeIfAbsent(metricsName, key -> new ArrayList<>());

            runningRules.add(runningRule);
        });

        this.rules = newRules;
        this.runningContext = newRunningContext;
        this.alarmRuleRunningRuleMap = newAlarmRuleRunningRuleMap;
    }

    @Override
    public String value() {
        return settingsString;
    }

    public List<AlarmRule> getRules() {
        return this.rules.getRules();
    }

    public List<String> getWebHooks() {
        return this.rules.getWebhooks();
    }
}
