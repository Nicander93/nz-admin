package com.nz.admin.framework.realtime.web;

import com.nz.admin.framework.realtime.core.RealtimeConnectionRegistry;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 只接受心跳，业务消息统一由服务端发布端口下发。
 */
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeConnectionRegistry connectionRegistry;

    public RealtimeWebSocketHandler(RealtimeConnectionRegistry connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object value = session.getAttributes().get(RealtimeHandshakeInterceptor.PRINCIPAL_ATTRIBUTE);
        if (!(value instanceof RealtimePrincipal principal)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing realtime principal"));
            return;
        }
        connectionRegistry.connectWebSocket(session, principal);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equalsIgnoreCase(message.getPayload().trim())) {
            connectionRegistry.respondToPing(session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connectionRegistry.disconnectWebSocket(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        connectionRegistry.disconnectWebSocket(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
