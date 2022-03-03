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

import com.sun.java.swing.plaf.windows.resources.windows;
import org.apache.skywalking.oap.server.core.alarm.AlarmMessage;
import org.apache.skywalking.oap.server.core.alarm.MetaInAlarm;
import org.apache.skywalking.oap.server.core.analysis.metrics.DoubleValueHolder;
import org.apache.skywalking.oap.server.core.analysis.metrics.IntValueHolder;
import org.apache.skywalking.oap.server.core.analysis.metrics.LongValueHolder;
import org.apache.skywalking.oap.server.core.analysis.metrics.Metrics;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RunningRule represents each rule in running status. Based on the {@link AlarmRule} definition,
 *
 * @author wusheng
 */
public class RunningRule {
    private static final Logger logger = LoggerFactory.getLogger(RunningRule.class);
    private static DateTimeFormatter TIME_BUCKET_FORMATTER = DateTimeFormat.forPattern("yyyyMMddHHmm");

    private String ruleName;
    private int period; // 窗口大小 单位分钟
    private String metricsName;
    private final Threshold threshold; //告警阈值
    private final OP op;
    private final int countThreshold; // 一个时间窗口内达到告警阈值几次进行告警
    private final int silencePeriod; // 抑制周期
    // Metrics会
    private Map<MetaInAlarm /* 指标的一些元信息 */, Window/* 规则配置的时间窗口,内含多个样本窗口 */> windows;
    private volatile MetricsValueType valueType;
    private int targetScopeId;
    private List<String> includeNames;
    private List<String> excludeNames;
    private AlarmMessageFormatter formatter;

    public RunningRule(AlarmRule alarmRule) {
        metricsName = alarmRule.getMetricsName();
        this.ruleName = alarmRule.getAlarmRuleName();

        // Init the empty window for alarming rule.
        windows = new ConcurrentHashMap<>();

        period = alarmRule.getPeriod();

        threshold = new Threshold(alarmRule.getAlarmRuleName(), alarmRule.getThreshold());
        op = OP.get(alarmRule.getOp());

        this.countThreshold = alarmRule.getCount();
        this.silencePeriod = alarmRule.getSilencePeriod();

        this.includeNames = alarmRule.getIncludeNames();
        this.excludeNames = alarmRule.getExcludeNames();
        this.formatter = new AlarmMessageFormatter(alarmRule.getMessage());
    }

    /**
     * Receive metrics result from persistence, after it is saved into storage. In alarm, only minute dimensionality
     * metrics are expected to process.
     *
     * @param metrics
     */
    public void in(MetaInAlarm meta, Metrics metrics) {
        if (!meta.getMetricsName().equals(metricsName)) {
            //Don't match rule, exit.
            return;
        }

        if (CollectionUtils.isNotEmpty(includeNames)) {
            if (!includeNames.contains(meta.getName())) {
                return;
            }
        }

        if (CollectionUtils.isNotEmpty(excludeNames)) {
            if (excludeNames.contains(meta.getName())) {
                return;
            }
        }

        if (valueType == null) {
            if (metrics instanceof LongValueHolder) {
                valueType = MetricsValueType.LONG;
                threshold.setType(MetricsValueType.LONG);
            } else if (metrics instanceof IntValueHolder) {
                valueType = MetricsValueType.INT;
                threshold.setType(MetricsValueType.INT);
            } else if (metrics instanceof DoubleValueHolder) {
                valueType = MetricsValueType.DOUBLE;
                threshold.setType(MetricsValueType.DOUBLE);
            } else {
                return;
            }
            targetScopeId = meta.getScopeId();
        }

        if (valueType != null) {
            Window window = windows.get(meta);
            if (window == null) {
                window = new Window(period);
                LocalDateTime timebucket = TIME_BUCKET_FORMATTER.parseLocalDateTime(metrics.getTimeBucket() + "");
                window.moveTo(timebucket);
                windows.put(meta, window);
            }

            window.add(metrics);
        }
    }

    /**
     * Move the buffer window to give time.
     *
     * @param targetTime of moving target
     */
    public void moveTo(LocalDateTime targetTime) {
        windows.values().forEach(window -> window.moveTo(targetTime));
    }

    /**
     * Check the conditions, decide to whether trigger alarm.
     * 报警检测
     */
    public List<AlarmMessage> check() {
        List<AlarmMessage> alarmMessageList = new ArrayList<>(30);

        windows.entrySet().forEach(entry -> {
            // 一个指标元信息
            MetaInAlarm meta = entry.getKey();
            // 一个元信息对应一个窗口
            Window window = entry.getValue();
            // 窗口内部检测
            AlarmMessage alarmMessage = window.checkAlarm();
            // 存在告警消息
            if (alarmMessage != AlarmMessage.NONE) {
                alarmMessage.setScopeId(meta.getScopeId());
                alarmMessage.setScope(meta.getScope());
                alarmMessage.setName(meta.getName());
                alarmMessage.setId0(meta.getId0());
                alarmMessage.setId1(meta.getId1());
                alarmMessage.setRuleName(this.ruleName);
                alarmMessage.setAlarmMessage(formatter.format(meta));
                alarmMessage.setStartTime(System.currentTimeMillis());
                alarmMessageList.add(alarmMessage);
            }
        });

        return alarmMessageList;
    }



    /**
     * A metrics window, based on {@link AlarmRule#period}. This window slides with time, just keeps the recent
     * N(period) buckets.
     *
     * @author wusheng
     */
    public class Window {
        private LocalDateTime endTime;
        private int period; // 窗口范围 时间单位分钟
        private int counter;// 每进行一次告警 counter++;
        private int silenceCountdown; // 配置silence-period 降低到0发生告警则通知,否则抑制防止告警骚扰

        private LinkedList<Metrics> values; // 根据period切分的样本窗口,一个元素代表一分钟的统计监控数据聚合值
        private ReentrantLock lock = new ReentrantLock();

        public Window(int period) {
            this.period = period;
            // -1 means silence countdown is not running.
            silenceCountdown = -1;
            counter = 0;
            init();
        }

        // 根据传入的时间和endTime的差值更新values 简单说就是固定窗口随时间滑动
        public void moveTo(LocalDateTime current) {
            lock.lock();
            try {
                if (endTime == null) {
                    init();
                    endTime = current;
                } else {

                    int minutes = Minutes.minutesBetween(endTime, current).getMinutes();
                    if (minutes <= 0) {
                        //       一个value占一分钟
                        //          values


                        // ------|-----------|---------------
                        //    窗口起点       endtime
                        //             |
                        //           current
                        // 说明endTime无需移动
                        return;
                    }
                    if (minutes > values.size()) { // 说明现在的窗口太老,新建一批窗口

                        //                                下一个窗口大小
                        // ------|-----------|------------|---------
                        //    窗口起点       endtime
                        //                                      |
                        //                                  current

                        // 说明values窗口时间太旧
                        //  // ------ ----------- --|----------|---
                        //                      新窗口起点        |
                        //                                   新窗口endtime
                        //                                       |
                        //                                   current

                        init();
                    } else {

                        //                                下一个窗口大小
                        // ------|-----------|------------|---------
                        //    窗口起点       endtime    |
                        //                             |
                        //                           current
                        //

                        // values部分太老

                        //                 === 这部分老窗口未彻底过期 保留
                        //         ========这部分老窗口数据删除
                        // --------|-------|--|----------|-----------
                        //    窗口起点                 endtime
                        //                             |
                        //                            current


                        for (int i = 0; i < minutes; i++) {
                            // 固定窗口滑动  删除历史的采样  新增现在的采样
                            values.removeFirst();
                            values.addLast(null);
                        }
                    }
                    endTime = current;
                }
            } finally {
                lock.unlock();
            }
        }

        // 将agent上报的指标度量更新到指定的value样本窗口中
        public void add(Metrics metrics) {
            long bucket = metrics.getTimeBucket();

            LocalDateTime timebucket = TIME_BUCKET_FORMATTER.parseLocalDateTime(bucket + "");

            int minutes = Minutes.minutesBetween(timebucket, endTime).getMinutes();
            if (minutes == -1) {
                this.moveTo(timebucket);

            }

            lock.lock();
            try {
                if (minutes < 0) { // 当前窗口未来的数据 则需要更新窗口
                    moveTo(timebucket);
                    minutes = 0;
                }

                if (minutes >= values.size()) {
                    // too old data
                    // also should happen, but maybe if agent/probe mechanism time is not right.
                    return;
                }
                // 更新数据
                values.set(values.size() - minutes - 1, metrics);
            } finally {
                lock.unlock();
            }
        }

        // 根据窗口数据进行检查 返回告警消息
        public AlarmMessage checkAlarm() {
            // 存在告警匹配
            if (isMatch()) { // 产生阈值超出
                /**
                 * When
                 * 1. Metrics value threshold triggers alarm by rule
                 * 2. Counter reaches the count threshold;
                 * 3. Isn't in silence stage, judged by SilenceCountdown(!=0).
                 */
                counter++; // 经历多次检测 counter才会满足应告警次数大于countThreshold,才会触发告警通知
                // 满足告警
                if (counter >= countThreshold && silenceCountdown < 1) {
                    silenceCountdown = silencePeriod;

                    // set empty message, but new message
                    AlarmMessage message = new AlarmMessage();
                    return message;
                } else {
                    // 其他情况减少抑制周期
                    silenceCountdown--;
                }
            } else {
                silenceCountdown--;
                if (counter > 0) {
                    counter--;
                }
            }
            return AlarmMessage.NONE;
        }

        private boolean isMatch() {
            int matchCount = 0;
            for (Metrics metrics : values) {
                if (metrics == null) {
                    continue;
                }

                switch (valueType) {
                    case LONG:
                        long lvalue = ((LongValueHolder)metrics).getValue();
                        long lexpected = RunningRule.this.threshold.getLongThreshold();
                        switch (op) {
                            case GREATER:
                                if (lvalue > lexpected)
                                    matchCount++;
                                break;
                            case LESS:
                                if (lvalue < lexpected)
                                    matchCount++;
                                break;
                            case EQUAL:
                                if (lvalue == lexpected)
                                    matchCount++;
                                break;
                        }
                        break;
                    case INT:
                        int ivalue = ((IntValueHolder)metrics).getValue();
                        int iexpected = RunningRule.this.threshold.getIntThreshold();
                        switch (op) {
                            case LESS:
                                if (ivalue < iexpected)
                                    matchCount++;
                                break;
                            case GREATER:
                                if (ivalue > iexpected)
                                    matchCount++;
                                break;
                            case EQUAL:
                                if (ivalue == iexpected)
                                    matchCount++;
                                break;
                        }
                        break;
                    case DOUBLE:
                        double dvalue = ((DoubleValueHolder)metrics).getValue();
                        double dexpected = RunningRule.this.threshold.getDoubleThreadhold();
                        switch (op) {
                            case EQUAL:
                                // NOTICE: double equal is not reliable in Java,
                                // match result is not predictable
                                if (dvalue == dexpected)
                                    matchCount++;
                                break;
                            case GREATER:
                                if (dvalue > dexpected)
                                    matchCount++;
                                break;
                            case LESS:
                                if (dvalue < dexpected)
                                    matchCount++;
                                break;
                        }
                        break;
                }
            }

            // Reach the threshold in current bucket.
            return matchCount >= countThreshold;
        }

        private void init() {
            values = new LinkedList();
            // period 单位为分钟表示窗口的大小
            for (int i = 0; i < period; i++) {
                // values为一个样本窗口,表示1分钟大小
                values.add(null);
            }
        }
    }
}
