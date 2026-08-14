package com.nz.admin.framework.realtime.config;

import com.nz.admin.framework.realtime.web.RealtimeEndpoints;
import com.nz.admin.framework.realtime.web.RealtimeHandshakeInterceptor;
import com.nz.admin.framework.realtime.web.RealtimeWebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * 注册原生 WebSocket 端点。
 */
public class RealtimeWebSocketConfiguration implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeInterceptor interceptor;
    private final List<String> allowedOrigins;

    public RealtimeWebSocketConfiguration(RealtimeWebSocketHandler handler,
                                          RealtimeHandshakeInterceptor interceptor,
                                          List<String> allowedOrigins) {
        this.handler = handler;
        this.interceptor = interceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketHandlerRegistration registration = registry
                .addHandler(handler, RealtimeEndpoints.WEB_SOCKET_PATH)
                .addInterceptors(interceptor);
        if (!allowedOrigins.isEmpty()) {
            registration.setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
        }
    }
}
