package com.nz.admin.framework.realtime.web;

import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * 在 WebSocket 握手阶段消费一次性票据。
 */
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PRINCIPAL_ATTRIBUTE =
            RealtimeHandshakeInterceptor.class.getName() + ".principal";

    private final RealtimeTicketService ticketService;

    public RealtimeHandshakeInterceptor(RealtimeTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");
        Optional<RealtimePrincipal> principal =
                ticketService.consume(ticket, RealtimeTransport.WEBSOCKET);
        principal.ifPresent(value -> attributes.put(PRINCIPAL_ATTRIBUTE, value));
        return principal.isPresent();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后不保留额外资源。
    }
}
