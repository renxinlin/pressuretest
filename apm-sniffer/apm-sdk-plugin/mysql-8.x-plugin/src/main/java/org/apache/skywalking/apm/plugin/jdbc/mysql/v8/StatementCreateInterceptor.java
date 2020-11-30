package org.apache.skywalking.apm.plugin.jdbc.mysql.v8;

import com.mysql.cj.jdbc.ClientPreparedStatement;
import com.mysql.cj.jdbc.StatementImpl;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.TracingContext;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

/**
 * @description:
 * @author: renxl
 * @create: 2020-11-28 23:31
 */
public class StatementCreateInterceptor implements InstanceConstructorInterceptor {
    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {

        TracingContext abstractTracerContext = (TracingContext) ContextManager.get();
        boolean pressureTest = abstractTracerContext.getSegment().isPressureTest();
        if(pressureTest) {
            // todo 验证版本参数  晚于构造方法
            if (objInst instanceof ClientPreparedStatement) {
                // 第三个参数
                allArguments[2] = "shadow_" + allArguments[2];
                ((ClientPreparedStatement) objInst).setCurrentCatalog((String) allArguments[2]);
            }
            if (objInst instanceof StatementImpl) {
                // 第二个参数
                allArguments[1] = "shadow_" + allArguments[1];
                ((StatementImpl) objInst).setCurrentCatalog((String) allArguments[1]);

            }
        }



    }
}
