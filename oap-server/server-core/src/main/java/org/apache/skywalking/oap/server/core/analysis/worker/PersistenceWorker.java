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

package org.apache.skywalking.oap.server.core.analysis.worker;

import java.util.*;
import org.apache.skywalking.oap.server.core.analysis.data.*;
import org.apache.skywalking.oap.server.core.storage.StorageData;
import org.apache.skywalking.oap.server.core.worker.AbstractWorker;
import org.apache.skywalking.oap.server.library.client.request.PrepareRequest;
import org.apache.skywalking.oap.server.library.module.ModuleDefineHolder;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public abstract class PersistenceWorker<INPUT extends StorageData, CACHE extends Window<INPUT>> extends AbstractWorker<INPUT> {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceWorker.class);

    PersistenceWorker(ModuleDefineHolder moduleDefineHolder) {
        super(moduleDefineHolder);
    }

    void onWork(INPUT input) {
        cacheData(input);
    }

    public abstract void cacheData(INPUT input);

    public abstract CACHE getCache();

    public abstract void endOfRound(long tookTime);

    public boolean flushAndSwitch() {
        boolean isSwitch;
        try {
            if (isSwitch = getCache().trySwitchPointer()) {
                getCache().switchPointer();
            }
        } finally {
            getCache().trySwitchPointerFinally();
        }
        return isSwitch;
    }

    public abstract void prepareBatch(Collection<INPUT> lastCollection, List<PrepareRequest> prepareRequests);

    public final void buildBatchRequests(List<PrepareRequest> prepareRequests) {
        try {
            SWCollection<INPUT> last = getCache().getLast();
            while (last.isWriting()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.warn("thread wake up");
                }
            }

            if (last.collection() != null) {
                prepareBatch(last.collection(), prepareRequests);
            }
        } finally {
            getCache().finishReadingLast();
        }
    }
}
