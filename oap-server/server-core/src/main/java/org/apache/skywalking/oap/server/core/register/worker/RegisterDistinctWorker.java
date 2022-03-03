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

package org.apache.skywalking.oap.server.core.register.worker;

import java.util.*;
import org.apache.skywalking.apm.commons.datacarrier.DataCarrier;
import org.apache.skywalking.apm.commons.datacarrier.consumer.*;
import org.apache.skywalking.oap.server.core.UnexpectedException;
import org.apache.skywalking.oap.server.core.register.RegisterSource;
import org.apache.skywalking.oap.server.core.worker.AbstractWorker;
import org.apache.skywalking.oap.server.library.module.ModuleDefineHolder;
import org.slf4j.*;

/**
 * 注册去重工作者
 * @author peng-yongsheng
 */
public class RegisterDistinctWorker extends AbstractWorker<RegisterSource> {

    private static final Logger logger = LoggerFactory.getLogger(RegisterDistinctWorker.class);

    private final AbstractWorker<RegisterSource> nextWorker;
    private final DataCarrier<RegisterSource> dataCarrier;
    private final Map<RegisterSource, RegisterSource> sources;
    private int messageNum;

    RegisterDistinctWorker(ModuleDefineHolder moduleDefineHolder, AbstractWorker<RegisterSource> nextWorker) {
        super(moduleDefineHolder);
        this.nextWorker = nextWorker;
        this.sources = new HashMap<>();
        // 创建worker自己的缓冲区
        this.dataCarrier = new DataCarrier<>(1, 1000);
        // 消费池名称
        String name = "REGISTER_L1";
        // 消费线程数
        int size = BulkConsumePool.Creator.recommendMaxSize() / 8;
        if (size == 0) {
            size = 1;
        }
        // 消费池
        BulkConsumePool.Creator creator = new BulkConsumePool.Creator(name, size, 200/*缓冲区无数据则间隔时间200ms*/);
        try {
            // 第一次创建的时候
            ConsumerPoolFactory.INSTANCE.createIfAbsent(name, creator);
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
        // dataCarrier 的消费池和消费逻辑
        this.dataCarrier.consume(ConsumerPoolFactory.INSTANCE.get(name), new AggregatorConsumer(this));
    }

    @Override public final void in(RegisterSource source) {
        source.resetEndOfBatch();
        // 写入缓冲区异步消费
        dataCarrier.produce(source);
    }

    private void onWork(RegisterSource source) {
        messageNum++;

        if (!sources.containsKey(source)) {
            // 第一次出现则在加入一道真正处理的缓冲区
            sources.put(source, source);
        } else {
            // 已经存在则合并
            sources.get(source).combine(source);
        }
        // 消息数量超过1000或者一个批次的数据
        if (messageNum >= 1000 || source.isEndOfBatch()) {
            sources.values().forEach(nextWorker::in);
            sources.clear();
            messageNum = 0;
        }
    }

    private class AggregatorConsumer implements IConsumer<RegisterSource> {

        private final RegisterDistinctWorker aggregator;

        private AggregatorConsumer(RegisterDistinctWorker aggregator) {
            this.aggregator = aggregator;
        }

        @Override public void init() {
        }

        @Override public void consume(List<RegisterSource> sources) {
            Iterator<RegisterSource> sourceIterator = sources.iterator();

            int i = 0;
            while (sourceIterator.hasNext()) {
                RegisterSource source = sourceIterator.next();
                i++;
                if (i == sources.size()) {
                    source.asEndOfBatch();
                }
                aggregator.onWork(source);
            }
        }

        @Override public void onError(List<RegisterSource> sources, Throwable t) {
            logger.error(t.getMessage(), t);
        }

        @Override public void onExit() {
        }
    }
}
