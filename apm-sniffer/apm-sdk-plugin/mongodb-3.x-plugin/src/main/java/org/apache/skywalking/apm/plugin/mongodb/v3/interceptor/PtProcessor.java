package org.apache.skywalking.apm.plugin.mongodb.v3.interceptor;

import com.mongodb.MongoNamespace;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.pt.FlagValue;

import java.lang.reflect.Field;

/**
 * @description:
 * @author: renxl
 * @create: 2020-12-04 17:32
 */
public class PtProcessor {
    private static final ILog logger = LogManager.getLogger(PtProcessor.class);

    public  static void processor(Object[] allArguments) {
        // 压测流量处理
        if (FlagValue.isPt()) {
            Object allArgument = allArguments[0];
            Field[] fields = allArgument.getClass().getDeclaredFields();
            try {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].getName().equals("databaseName")) {
                        // 确定存在才修改Accessible
                        fields[i].setAccessible(true);
                        String databaseName = (String) fields[i].get(allArgument);
                        // 请求级别数据 if属于防御性编程
                        if (!databaseName.contains(FlagValue.PT_ROUTE_PREFIX)) {
                            databaseName = FlagValue.PT_ROUTE_PREFIX + databaseName;
                        }
                        fields[i].set(allArgument, databaseName);
                    }
                    //  RenameCollectionOperation rename 唯一特殊的一个[originalNamespace,newNamespace] 一般在生产中不存在这种操作,直接这样操作可能是致命的
                    if (fields[i].getName().equals("namespace")
                            || fields[i].getName().equals("originalNamespace")
                            || fields[i].getName().equals("newNamespace")) {
                        fields[i].setAccessible(true);
                        MongoNamespace namespace = (MongoNamespace) fields[i].get(allArgument);
                        // 请求级别数据 if属于防御性编程
                        if (!namespace.getDatabaseName().contains(FlagValue.PT_ROUTE_PREFIX)) {
                            Class<? extends MongoNamespace> namespaceClass = namespace.getClass();
                            Field databaseNameField = namespaceClass.getDeclaredField("databaseName");
                            Field fullNameField = namespaceClass.getDeclaredField("fullName");
                            databaseNameField.setAccessible(true);
                            fullNameField.setAccessible(true);
                            databaseNameField.set(namespace, FlagValue.PT_ROUTE_PREFIX + namespace.getDatabaseName());
                            fullNameField.set(namespace, FlagValue.PT_ROUTE_PREFIX + namespace.getFullName());
                        }
                    }
                }
            } catch (NoSuchFieldException e) {
                logger.error("mongoDb 压测流量处理失败..", e);
            } catch (IllegalAccessException e) {
                logger.error("mongoDb 压测流量处理失败...", e);
            }
        }
    }

}
