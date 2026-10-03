package com.nz.admin.framework.auth.config;

import cn.dev33.satoken.dao.SaTokenDao;

import com.nz.admin.framework.auth.core.RedisSaTokenDao;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 显式集群模式共享会话，Redis 不可用时不使用本地登录态。 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnProperty(name = "nz.cluster.enabled", havingValue = "true")
public class ClusterAuthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao redisSaTokenDao(StringRedisTemplate redis, Environment env) {
        return new RedisSaTokenDao(redis, env.getProperty("nz.cache.key-prefix", "nz-admin"));
    }
}
