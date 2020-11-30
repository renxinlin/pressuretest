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
 */

package org.apache.skywalking.oap.server.core.storage.model;

import java.lang.reflect.Field;
import java.util.*;
import lombok.Getter;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.annotation.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class StorageModels implements IModelGetter, IModelSetter, IModelOverride {

    private static final Logger logger = LoggerFactory.getLogger(StorageModels.class);

    @Getter private final List<Model> models;

    public StorageModels() {
        this.models = new LinkedList<>();
    }

    @Override public Model putIfAbsent(Class aClass, int scopeId, Storage storage, boolean record) {
        // Check this scope id is valid.
        DefaultScopeDefine.nameOf(scopeId);

        for (Model model : models) {
            if (model.getName().equals(storage.getModelName())) {
                return model;
            }
        }

        List<ModelColumn> modelColumns = new LinkedList<>();
        retrieval(aClass, storage.getModelName(), modelColumns);

        Model model = new Model(storage.getModelName(), modelColumns, storage.isCapableOfTimeSeries(), storage.isDeleteHistory(), scopeId, storage.getDownsampling(), record);
        models.add(model);

        return model;
    }

    private void retrieval(Class clazz, String modelName, List<ModelColumn> modelColumns) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                modelColumns.add(new ModelColumn(new ColumnName(column.columnName()), field.getType(), column.matchQuery(), column.content()));
                if (logger.isDebugEnabled()) {
                    logger.debug("The field named {} with the {} type", column.columnName(), field.getType());
                }
                if (column.isValue()) {
                    ValueColumnIds.INSTANCE.putIfAbsent(modelName, column.columnName(), column.function());
                }
            }
        }

        if (Objects.nonNull(clazz.getSuperclass())) {
            retrieval(clazz.getSuperclass(), modelName, modelColumns);
        }
    }

    @Override public void overrideColumnName(String columnName, String newName) {
        models.forEach(model -> model.getColumns().forEach(column -> {
            ColumnName existColumnName = column.getColumnName();
            String name = existColumnName.getName();
            if (name.equals(columnName)) {
                existColumnName.setStorageName(newName);
                logger.debug("Model {} column {} has been override. The new column name is {}.", model.getName(), name, newName);
            }
        }));
    }
}
