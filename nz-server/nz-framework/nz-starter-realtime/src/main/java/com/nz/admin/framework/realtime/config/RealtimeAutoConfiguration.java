package com.nz.admin.framework.realtime.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.realtime.core.InMemoryRealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimeConnectionRegistry;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.web.RealtimeHandshakeInterceptor;
import com.nz.admin.framework.realtime.web.RealtimeSseController;
import com.nz.admin.framework.realtime.web.RealtimeWebSocketHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.time.Clock;

/**
 * 实时通信自动装配。
 */
@AutoConfiguration(after=com.nz.admin.framework.cache.config.AtomicStateAutoConfiguration.class)
@EnableWebSocket
@EnableConfigurationProperties(RealtimeProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "nz.realtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RealtimeTicketService realtimeTicketService(RealtimeProperties properties,
            org.springframework.beans.factory.ObjectProvider<com.nz.admin.framework.cache.core.AtomicStateStore> states,
            ObjectMapper mapper) {
        var store=states.getIfAvailable();
        return store==null ? new InMemoryRealtimeTicketService(properties.getTicketTtl(),Clock.systemUTC())
                : new com.nz.admin.framework.realtime.core.SharedRealtimeTicketService(store,mapper,properties.getTicketTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    RealtimeConnectionRegistry realtimeConnectionRegistry(
            ObjectMapper objectMapper,
            RealtimeProperties properties) {
        return new RealtimeConnectionRegistry(objectMapper, properties.getSseTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(RealtimePublisher.class)
    RealtimePublisher realtimePublisher(RealtimeConnectionRegistry registry) {
        return registry;
    }

    @Bean
    @org.springframework.context.annotation.Primary
    @ConditionalOnProperty(name="nz.cluster.enabled",havingValue="true")
    com.nz.admin.framework.realtime.core.RedisRealtimePublisher clusterRealtimePublisher(
            RealtimeConnectionRegistry registry,org.springframework.data.redis.core.StringRedisTemplate redis,
            ObjectMapper mapper,org.springframework.core.env.Environment env) {
        return new com.nz.admin.framework.realtime.core.RedisRealtimePublisher(registry,redis,mapper,
                env.getProperty("nz.cache.key-prefix","nz-admin")+":realtime");
    }

    @Bean
    @ConditionalOnProperty(name="nz.cluster.enabled",havingValue="true")
    org.springframework.data.redis.listener.RedisMessageListenerContainer realtimeRedisListener(
            com.nz.admin.framework.realtime.core.RedisRealtimePublisher publisher,
            org.springframework.data.redis.connection.RedisConnectionFactory factory,
            org.springframework.core.env.Environment env) {
        var listener=new org.springframework.data.redis.listener.RedisMessageListenerContainer();
        listener.setConnectionFactory(factory);
        listener.addMessageListener(publisher,new org.springframework.data.redis.listener.ChannelTopic(
                env.getProperty("nz.cache.key-prefix","nz-admin")+":realtime"));
        return listener;
    }

    @Bean
    RealtimeSseController realtimeSseController(
            RealtimeTicketService ticketService,
            RealtimeConnectionRegistry registry) {
        return new RealtimeSseController(ticketService, registry);
    }

    @Bean
    RealtimeHandshakeInterceptor realtimeHandshakeInterceptor(RealtimeTicketService ticketService) {
        return new RealtimeHandshakeInterceptor(ticketService);
    }

    @Bean
    RealtimeWebSocketHandler realtimeWebSocketHandler(RealtimeConnectionRegistry registry) {
        return new RealtimeWebSocketHandler(registry);
    }

    @Bean
    RealtimeWebSocketConfiguration realtimeWebSocketConfiguration(
            RealtimeWebSocketHandler handler,
            RealtimeHandshakeInterceptor interceptor,
            RealtimeProperties properties) {
        return new RealtimeWebSocketConfiguration(
                handler,
                interceptor,
                properties.getAllowedOrigins()
        );
    }
}
