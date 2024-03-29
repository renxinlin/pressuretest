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

package org.apache.skywalking.oap.server.core.source;

import java.io.IOException;
import lombok.Getter;
import org.apache.skywalking.oap.server.core.analysis.DispatcherManager;

/**
 * @author peng-yongsheng
 */
public class SourceReceiverImpl implements SourceReceiver {
    @Getter
    private final DispatcherManager dispatcherManager;

    public SourceReceiverImpl() {
        this.dispatcherManager = new DispatcherManager();
    }

    @Override public void receive(Source source) {
        /*
                OAL动态生成的调度器，也会在这进行分发
         */
        dispatcherManager.forward(source);
    }

    public void scan() throws IOException, InstantiationException, IllegalAccessException {
        dispatcherManager.scan();
    }
}
