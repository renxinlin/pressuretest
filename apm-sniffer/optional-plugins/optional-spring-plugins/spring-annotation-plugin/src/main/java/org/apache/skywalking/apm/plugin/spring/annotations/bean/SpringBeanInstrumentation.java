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

package org.apache.skywalking.apm.plugin.spring.annotations.bean;

import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.spring.annotations.AbstractSpringBeanInstrumentation;

import static org.apache.skywalking.apm.agent.core.plugin.match.ClassAnnotationMatch.byClassAnnotationMatch;

/**
 *
 */
public class SpringBeanInstrumentation extends AbstractSpringBeanInstrumentation {

    public static final String ENHANCE_ANNOTATION = "org.springframework.context.annotation.Bean";

    @Override protected ClassMatch enhanceClass() {
        return byClassAnnotationMatch(new String[] {ENHANCE_ANNOTATION});
    }
}
