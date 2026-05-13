package com.nz.admin.framework.cache.config;

import com.nz.admin.framework.cache.core.CacheKeyBuilder;
import com.nz.admin.framework.cache.properties.CacheFrameworkProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class NzCacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NzCacheAutoConfiguration.class))
            .withBean(LettuceConnectionFactory.class, () -> new LettuceConnectionFactory("localhost", 6379));

    @Test
    void shouldRegisterCacheBeans() {
        contextRunner.withPropertyValues("nz.cache.key-prefix=nz:test")
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheFrameworkProperties.class);
                    assertThat(context).hasSingleBean(CacheKeyBuilder.class);
                    assertThat(context).hasSingleBean(RedisTemplate.class);
                    assertThat(context).hasSingleBean(RedisCacheConfiguration.class);

                    CacheKeyBuilder cacheKeyBuilder = context.getBean(CacheKeyBuilder.class);
                    assertThat(cacheKeyBuilder.build("dict", "user")).isEqualTo("nz:test:dict:user");
                });
    }
}
