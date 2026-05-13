package com.nz.admin.framework.cache.config;

import com.nz.admin.framework.cache.core.CacheKeyBuilder;
import com.nz.admin.framework.cache.properties.CacheFrameworkProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存自动配置。
 */
@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(CacheFrameworkProperties.class)
@ConditionalOnClass(RedisTemplate.class)
public class NzCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheKeyBuilder cacheKeyBuilder(CacheFrameworkProperties properties) {
        return new CacheKeyBuilder(properties);
    }

    @Bean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        return redisTemplate;
    }

    @Bean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public RedisCacheConfiguration redisCacheConfiguration(CacheFrameworkProperties properties) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .computePrefixWith(cacheName -> properties.getKeyPrefix() + ":" + cacheName + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        if (!properties.isCacheNullValues()) {
            configuration = configuration.disableCachingNullValues();
        }
        return configuration;
    }

    @Bean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration redisCacheConfiguration) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}
