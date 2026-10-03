package com.nz.admin.framework.cache.config;

import com.nz.admin.framework.cache.core.*;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 显式开启集群后只使用共享状态，缺少 Redis 配置时启动失败。 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
public class AtomicStateAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(AtomicStateStore.class)
    @ConditionalOnProperty(
            name = "nz.cluster.enabled",
            havingValue = "false",
            matchIfMissing = true)
    public AtomicStateStore localAtomicStateStore() {
        return new InMemoryAtomicStateStore();
    }

    @Bean
    @ConditionalOnMissingBean(AtomicStateStore.class)
    @ConditionalOnProperty(name = "nz.cluster.enabled", havingValue = "true")
    public AtomicStateStore redisAtomicStateStore(StringRedisTemplate redis, Environment env) {
        return new RedisAtomicStateStore(redis, env.getProperty("nz.cache.key-prefix", "nz-admin"));
    }
}
