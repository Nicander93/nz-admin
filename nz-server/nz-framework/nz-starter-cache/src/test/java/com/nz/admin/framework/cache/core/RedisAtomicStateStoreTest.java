package com.nz.admin.framework.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
class RedisAtomicStateStoreTest {
    @Test
    void independentNodesShareAtomicConsumptionAndLimits() throws Exception {
        int port = Integer.parseInt(System.getenv("NZ_TEST_REDIS_PORT"));
        var factoryA = new LettuceConnectionFactory("127.0.0.1", port);
        var factoryB = new LettuceConnectionFactory("127.0.0.1", port);
        factoryA.afterPropertiesSet();
        factoryB.afterPropertiesSet();
        var prefix = "test-" + UUID.randomUUID();
        var nodeA = new RedisAtomicStateStore(new StringRedisTemplate(factoryA), prefix);
        var nodeB = new RedisAtomicStateStore(new StringRedisTemplate(factoryB), prefix);
        var ttl = Duration.ofSeconds(10);
        var pool = Executors.newFixedThreadPool(16);
        try {
            assertThat(nodeA.putIfAbsent("ticket", "identity", ttl)).isTrue();
            assertThat(nodeB.take("ticket")).isEqualTo("identity");
            assertThat(nodeA.take("ticket")).isNull();
            var accepted = new AtomicInteger();
            var tasks = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 64; i++) {
                var node = i % 2 == 0 ? nodeA : nodeB;
                tasks.add(
                        pool.submit(
                                () -> {
                                    if (node.putIfAbsent("once", "value", ttl))
                                        accepted.incrementAndGet();
                                }));
            }
            for (var task : tasks) task.get(10, TimeUnit.SECONDS);
            assertThat(accepted.get()).isEqualTo(1);
            assertThat(nodeB.compareAndSet("once", "wrong", "new", ttl)).isFalse();
            assertThat(nodeB.compareAndSet("once", "value", "new", ttl)).isTrue();
            assertThat(nodeA.deleteIfValue("once", "value")).isFalse();
            assertThat(nodeA.deleteIfValue("once", "new")).isTrue();
            for (int i = 1; i <= 10; i++)
                assertThat((i % 2 == 0 ? nodeA : nodeB).increment("limit", ttl)).isEqualTo(i);
        } finally {
            pool.shutdownNow();
            factoryA.destroy();
            factoryB.destroy();
        }
    }
}
