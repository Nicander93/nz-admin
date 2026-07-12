package com.nz.admin.framework.monitor.redis;

import com.nz.admin.framework.monitor.core.RedisMonitorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringDataRedisMonitorStatusProviderTest {

    @Test
    void shouldReadRedisInfoAndDbSize() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        Properties info = new Properties();
        info.setProperty("redis_version", "7.2.5");
        info.setProperty("redis_mode", "standalone");
        info.setProperty("connected_clients", "4");
        info.setProperty("used_memory", "2048");
        when(factory.getConnection()).thenReturn(connection);
        when(connection.info()).thenReturn(info);
        when(connection.dbSize()).thenReturn(12L);

        RedisMonitorStatus status = new SpringDataRedisMonitorStatusProvider(factory).getStatus();

        assertThat(status.isOk()).isTrue();
        assertThat(status.getVersion()).isEqualTo("7.2.5");
        assertThat(status.getMode()).isEqualTo("standalone");
        assertThat(status.getConnectedClients()).isEqualTo(4L);
        assertThat(status.getUsedMemoryBytes()).isEqualTo(2048L);
        assertThat(status.getKeyCount()).isEqualTo(12L);
    }

    @Test
    void shouldReportConnectionFailureWithoutThrowing() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenThrow(new IllegalStateException("redis down"));

        RedisMonitorStatus status = new SpringDataRedisMonitorStatusProvider(factory).getStatus();

        assertThat(status.isOk()).isFalse();
        assertThat(status.getMessage()).contains("redis down");
    }
}