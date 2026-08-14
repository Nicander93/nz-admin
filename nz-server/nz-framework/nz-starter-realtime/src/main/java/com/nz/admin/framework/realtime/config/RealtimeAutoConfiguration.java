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
@AutoConfiguration
@EnableWebSocket
@EnableConfigurationProperties(RealtimeProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "nz.realtime", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RealtimeTicketService realtimeTicketService(RealtimeProperties properties) {
        return new InMemoryRealtimeTicketService(properties.getTicketTtl(), Clock.systemUTC());
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
