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

package org.apache.skywalking.oap.server.core.analysis;

import java.lang.annotation.Annotation;
import java.util.*;
import org.apache.skywalking.oap.server.core.annotation.AnnotationListener;

/**
 * Disable definition scanner and register.
 *
 * @author wusheng
 */
public class DisableRegister implements AnnotationListener {
    public static DisableRegister INSTANCE = new DisableRegister();
    private Set<String> disableEntitySet = new HashSet<>();

    private DisableRegister() {
    }

    @Override public Class<? extends Annotation> annotation() {
        return MultipleDisable.class;
    }

    @Override public void notify(Class aClass) {
        MultipleDisable annotation = (MultipleDisable)aClass.getAnnotation(MultipleDisable.class);
        Disable[] valueList = annotation.value();
        if (valueList != null) {
            for (Disable disable : valueList) {
                add(disable.value());
            }
        }
    }

    public void add(String name) {
        disableEntitySet.add(name);
    }

    public boolean include(String name) {
        return disableEntitySet.contains(name);
    }

    public static class SingleDisableScanListener implements AnnotationListener {
        @Override public Class<? extends Annotation> annotation() {
            return Disable.class;
        }

        @Override public void notify(Class aClass) {
            String name = ((Disable)aClass.getAnnotation(Disable.class)).value();
            DisableRegister.INSTANCE.disableEntitySet.add(name);
        }
    }
}
