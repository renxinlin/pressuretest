package org.apache.skywalking.apm.agent.core.pt;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.TracingContext;

/**
 * @description:
 * @author: renxl
 * @create: 2020-12-03 11:48
 */
public class FlagValue {
    /**
     * 压测资源前缀
     */
    public static  final  String PT_ROUTE_PREFIX = "shadow_";
    /**
     * 判断是否是压测环境
     * @return
     */
    public  static boolean isPt(){
        TracingContext abstractTracerContext = (TracingContext) ContextManager.get();
        return abstractTracerContext.getSegment().isPressureTest();
    }
}
