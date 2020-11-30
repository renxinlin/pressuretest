package org.apache.skywalking.apm.plugin.jdbc.mysql.v6;

import com.mysql.cj.jdbc.StatementImpl;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.TracingContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description:
 * @author: renxl
 * @create: 2020-11-28 23:31
 */
public class StatementCreateInterceptor implements InstanceConstructorInterceptor {
    private AtomicBoolean changed = new AtomicBoolean(false);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {


        TracingContext abstractTracerContext = (TracingContext) ContextManager.get();
        boolean pressureTest = abstractTracerContext.getSegment().isPressureTest();
        if (!pressureTest) {
            return;
        }

        if (changed.get()) {
            return;
        }

        if (objInst instanceof StatementImpl) {
            if (allArguments.length != 2) {
                return;
            }
            changed.compareAndSet(false, true);
            // 第二个参数 后期优化 改成连接connection的创建statement方法 去修改 替换反射
            allArguments[1] = "shadow_" + allArguments[1];
            try {

                Class<?> clazz = Class.forName("com.mysql.cj.jdbc.StatementImpl");
                Field field = clazz.getDeclaredField("currentCatalog");
                field.set(objInst, allArguments[1]);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("数据库连接层影子路由失败." + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("数据库连接层影子路由失败.." + e.getMessage());
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("数据库连接层影子路由失败..." + e.getMessage());
            }

        }

    }
}
