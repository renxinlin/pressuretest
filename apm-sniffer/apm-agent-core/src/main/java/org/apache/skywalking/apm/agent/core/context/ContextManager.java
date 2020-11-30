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

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.boot.*;
import org.apache.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.apache.skywalking.apm.agent.core.context.trace.*;
import org.apache.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.apache.skywalking.apm.agent.core.logging.api.*;
import org.apache.skywalking.apm.agent.core.sampling.SamplingService;
import org.apache.skywalking.apm.util.StringUtil;

import static org.apache.skywalking.apm.agent.core.conf.Config.Agent.OPERATION_NAME_THRESHOLD;

/**
 * http://www.iocoder.cn/categories/SkyWalking/
 *
 * https://github.com/apache/skywalking/blob/5.x/docs/cn/Plugin-Development-Guide-CN.md
 *
 *
 * {@link ContextManager} controls the whole context of {@link TraceSegment}. Any {@link TraceSegment} relates to
 * single-thread, so this context use {@link ThreadLocal} to maintain the context, and make sure, since a {@link
 * TraceSegment} starts, all ChildOf spans are in the same context. <p> What is 'ChildOf'?
 * https://github.com/opentracing/specification/blob/master/specification.md#references-between-spans
 *
 * <p> Also, {@link ContextManager} delegates to all {@link AbstractTracerContext}'s major methods.
 *
 * @author wusheng
 */
public class ContextManager implements BootService {
    private static final ILog logger = LogManager.getLogger(ContextManager.class);
    /**
     *  这个上下文是核心上下文 用户span的管理
     */
    private static ThreadLocal<AbstractTracerContext> CONTEXT = new ThreadLocal<AbstractTracerContext>();
    /**
     * 这个上下文 是辅助功能 链路上下文中提供数据传播
     * 比如tomcat接收请求 在某个拦截器判断是转发请求则设置转发请求标记而在之后某些拦截器判断是转发则不作链路上的一些吹
     * 针对不同的拦截器可以提供不同的作用
     */
    private static ThreadLocal<RuntimeContext> RUNTIME_CONTEXT = new ThreadLocal<RuntimeContext>();

    /**
     * 用来辅助 AbstractTracerContext对应的CONTEXT创建工作
     */
    private static ContextManagerExtendService EXTEND_SERVICE;

    // 注意虽然是静态的 但还是线程级别 同时也是私有级别
    private static AbstractTracerContext getOrCreate(String operationName, boolean forceSampling) {
        AbstractTracerContext context = CONTEXT.get();
        if (context == null) {
            if (StringUtil.isEmpty(operationName)) {
                if (logger.isDebugEnable()) {
                    logger.debug("No operation name, ignore this trace.");
                }
                // 对于全链路压测 这种情况暂时不考虑 因为一般的中间件都有了operationName
                context = new IgnoredTracerContext();
            } else {
                // 如果服务和服务实例对应字典都存在 则创建tracecontext 否则还是创建ignore上下文
                if (RemoteDownstreamConfig.Agent.SERVICE_ID != DictionaryUtil.nullValue()
                    && RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID != DictionaryUtil.nullValue()
                ) {
                    if (EXTEND_SERVICE == null) {
                        EXTEND_SERVICE = ServiceManager.INSTANCE.findService(ContextManagerExtendService.class);
                    }
                    // 创建tracecontext  确保不能创建IgnoredTracerContext 否则我的压测标可能丢失
                    context = EXTEND_SERVICE.createTraceContext(operationName, forceSampling);
                } else {
                    // 对于全链路压测 这种情况暂时不考虑 因为一般的service都会有相关的serviceId 和instanceId
                    /**
                     * Can't register to collector, no need to trace anything.
                     */
                    context = new IgnoredTracerContext();
                }
            }
            CONTEXT.set(context);
        }
        return context;
    }

    public static AbstractTracerContext get() {
        return CONTEXT.get();
    }



    /**
     * @return the first global trace id if needEnhance. Otherwise, "N/A".
     */
    public static String getGlobalTraceId() {
        AbstractTracerContext segment = CONTEXT.get();
        if (segment == null) {
            return "N/A";
        } else {
            return segment.getReadableGlobalTraceId();
        }
    }

    /**
     * 一般是从其他进程进来的 需要提取请求上下文
     * @param operationName
     * @param carrier
     * @return
     */
    public static AbstractSpan createEntrySpan(String operationName, ContextCarrier carrier) {
        AbstractSpan span;
        AbstractTracerContext context;
        operationName = StringUtil.cut(operationName, OPERATION_NAME_THRESHOLD);
        if (carrier != null && carrier.isValid()) {
            SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
            samplingService.forceSampled();
            context = getOrCreate(operationName, true);
            span = context.createEntrySpan(operationName);
            context.extract(carrier);
        } else {
            context = getOrCreate(operationName, false);
            span = context.createEntrySpan(operationName);
        }
        return span;
    }


    public static AbstractSpan createLocalSpan(String operationName) {
        operationName = StringUtil.cut(operationName, OPERATION_NAME_THRESHOLD);
        AbstractTracerContext context = getOrCreate(operationName, false);
        return context.createLocalSpan(operationName);
    }

    /**
     * 创建离开的 span 由于有span上下文载体 我理解 一般这里是下个进程还处理链路中 比如mq dubbo http ribbon等
     * @param operationName
     * @param carrier
     * @param remotePeer
     * @return
     */
    public static AbstractSpan createExitSpan(String operationName, ContextCarrier carrier, String remotePeer) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        operationName = StringUtil.cut(operationName, OPERATION_NAME_THRESHOLD);
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        context.inject(carrier);
        return span;
    }

    /**
     * 创建离开进程的span 我理解这里一般到结尾 比如到redis
     * @param operationName
     * @param remotePeer
     * @return
     */
    public static AbstractSpan createExitSpan(String operationName, String remotePeer) {
        operationName = StringUtil.cut(operationName, OPERATION_NAME_THRESHOLD);
        AbstractTracerContext context = getOrCreate(operationName, false);
        AbstractSpan span = context.createExitSpan(operationName, remotePeer);
        return span;
    }

    /**
     * 一般在exitSpan 注入 而且这类span往后还有被skywalking代理的地方
     * @param carrier
     */
    public static void inject(ContextCarrier carrier) {
        get().inject(carrier);
    }

    /**
     * 一般在entryspan 提取
     * @param carrier
     */
    public static void extract(ContextCarrier carrier) {
        if (carrier == null) {
            throw new IllegalArgumentException("ContextCarrier can't be null.");
        }
        if (carrier.isValid()) {
            get().extract(carrier);
        }
    }

    /**
     * 跨线程时 生成快照
     * @return
     */

    public static ContextSnapshot capture() {
        return get().capture();
    }

    /**
     * B线程通过ContextManager#continued操作完成上下文传递
     * @param snapshot
     */
    public static void continued(ContextSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("ContextSnapshot can't be null.");
        }
        if (snapshot.isValid() && !snapshot.isFromCurrent()) {
            get().continued(snapshot);
        }
    }

    public static AbstractTracerContext awaitFinishAsync(AbstractSpan span) {
        final AbstractTracerContext context = get();
        AbstractSpan activeSpan = context.activeSpan();
        if (span != activeSpan) {
            throw new RuntimeException("Span is not the active in current context.");
        }
        return context.awaitFinishAsync();
    }

    /**
     * 一般用于插件机制中的span在特定场景打标 比如异常场景
     * If not sure has the active span, use this method, will be cause NPE when has no active span,
     * use ContextManager::isActive method to determine whether there has the active span.
     */
    public static AbstractSpan activeSpan() {
        return get().activeSpan();
    }

    /**
     * 最终弹栈 上报
    * Recommend use ContextManager::stopSpan(AbstractSpan span), because in that way, 
    * the TracingContext core could verify this span is the active one, in order to avoid stop unexpected span.
    * If the current span is hard to get or only could get by low-performance way, this stop way is still acceptable.
    */
    public static void stopSpan() {
        final AbstractTracerContext context = get();
        stopSpan(context.activeSpan(),context);
    }

    public static void stopSpan(AbstractSpan span) {
        stopSpan(span, get());
    }

    private static void stopSpan(AbstractSpan span, final AbstractTracerContext context) {
        if (context.stopSpan(span)) {
            CONTEXT.remove();
            RUNTIME_CONTEXT.remove();
        }
    }

    @Override
    public void prepare() throws Throwable {

    }

    @Override
    public void boot() {
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override public void shutdown() throws Throwable {

    }

    public static boolean isActive() {
        return get() != null;
    }

    public static RuntimeContext getRuntimeContext() {
        RuntimeContext runtimeContext = RUNTIME_CONTEXT.get();
        if (runtimeContext == null) {
            runtimeContext = new RuntimeContext(RUNTIME_CONTEXT);
            RUNTIME_CONTEXT.set(runtimeContext);
        }

        return runtimeContext;
    }
}
