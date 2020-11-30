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

package org.apache.skywalking.e2e.topo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kezhenxu94
 */
public class TopoData {
    private List<Node> nodes;
    private List<Call> calls;

    public TopoData() {
        nodes = new ArrayList<>();
        calls = new ArrayList<>();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public TopoData setNodes(List<Node> nodes) {
        this.nodes = nodes;
        return this;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public TopoData setCalls(List<Call> calls) {
        this.calls = calls;
        return this;
    }

    @Override
    public String toString() {
        return "TopoData{" +
                "nodes=" + nodes +
                ", calls=" + calls +
                '}';
    }
}
