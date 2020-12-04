package org.apache.skywalking.apm.agent.core.pt;

import org.apache.skywalking.apm.agent.core.conf.Config;

/**
 * mysql 5,6,8:statement 请求级别
 * redis 2.x | redistemplate : StaticRoutingInfo 不支持集群，业务可能用一个库不支持select
 * redisson3.x: 支持单机 云 哨兵 主从  不支持集群
 * <p>
 * dubbo:携带压测标
 * mq: 目前支持，压测流量和真实流量在同一个topic
 * runnable: 目前不支持 但是打算支持
 * lambda: 目前不支持 但是打算支持
 * executors: 目前待测试
 * es: 已经完毕
 * mongodb 待实现
 * hbase: 待实现
 * Disruptor： 目前不支持 但是打算支持
 * d
 * <p>
 * xx-job: 打算研究支持
 * <p>
 * 阿里云ons: 必须要支持
 * hotkey: 目前不打算支持 但后期可能考虑支持
 * etcd: 目前不打算支持 但后期可能考虑支持
 * zk: 作为元数据 不打算支持
 *
 * @description:
 * @author: renxl
 * @create: 2020-12-03 15:39
 */
public class StaticRoutingInfo {
    /**
     * 默认值 : redis的db不是请求级别
     * <p>
     * <p>
     * 查看源码 可以发现select()是将db的信息发到了服务端
     * 之后每次请求 服务端根据socket stream /channel 识别对应的index
     * 所以这里将环境分为压测环境和真实环境;对应的数据库下标在这里
     * 后期Jedis 的datasource属性不再起作用 每次请求都会取这里的index
     */
    public static volatile Long redisDb = 0L;
    public static volatile Long ptRedisDb = 1L;



    public static void init() {
        initRedis();
    }

    private static void initRedis() {
        try {
            redisDb = Long.valueOf(Config.Agent.REDIS_DB);
            ptRedisDb = (redisDb + 1) % 16;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}
