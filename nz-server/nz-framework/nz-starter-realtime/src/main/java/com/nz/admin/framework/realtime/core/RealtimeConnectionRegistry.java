package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * 保存当前应用节点上的 SSE 与 WebSocket 连接。
 */
public class RealtimeConnectionRegistry implements RealtimePublisher, RealtimeConnectionManager {

    private final ConcurrentMap<String, SseConnection> sseConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketConnection> webSocketConnections = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Duration sseTimeout;

    public RealtimeConnectionRegistry(ObjectMapper objectMapper, Duration sseTimeout) {
        this.objectMapper = objectMapper;
        this.sseTimeout = sseTimeout;
    }

    public SseEmitter connectSse(RealtimePrincipal principal) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(sseTimeout.toMillis());
        sseConnections.put(connectionId, new SseConnection(principal, emitter));
        emitter.onCompletion(() -> sseConnections.remove(connectionId));
        emitter.onTimeout(() -> {
            sseConnections.remove(connectionId);
            emitter.complete();
        });
        emitter.onError(error -> sseConnections.remove(connectionId));
        sendSse(connectionId, RealtimeMessage.of(
                "connected",
                Map.of("connectionId", connectionId, "transport", RealtimeTransport.SSE.name())
        ));
        return emitter;
    }

    public void connectWebSocket(WebSocketSession session, RealtimePrincipal principal) throws IOException {
        webSocketConnections.put(session.getId(), new WebSocketConnection(principal, session));
        if (!sendWebSocket(session.getId(), RealtimeMessage.of(
                "connected",
                Map.of("connectionId", session.getId(), "transport", RealtimeTransport.WEBSOCKET.name())
        ))) {
            throw new IOException("WebSocket connection closed before initialization");
        }
    }

    public void disconnectWebSocket(String connectionId) {
        webSocketConnections.remove(connectionId);
    }

    public void respondToPing(String connectionId) {
        sendWebSocket(connectionId, RealtimeMessage.of("pong", Map.of()));
    }

    @Override
    public int publishToUser(Long tenantId, Long userId, RealtimeMessage message) {
        return publish(
                principal -> Objects.equals(principal.tenantId(), tenantId)
                        && Objects.equals(principal.userId(), userId),
                message
        );
    }

    @Override
    public int publishToTenant(Long tenantId, RealtimeMessage message) {
        return publish(principal -> Objects.equals(principal.tenantId(), tenantId), message);
    }

    @Override
    public int broadcast(RealtimeMessage message) {
        return publish(principal -> true, message);
    }

    @Override
    public RealtimeConnectionStats stats() {
        return new RealtimeConnectionStats(sseConnections.size(), webSocketConnections.size());
    }
    @Override
    public int disconnectUser(Long tenantId, Long userId) {
        int disconnected = 0;
        for (Map.Entry<String, SseConnection> entry : sseConnections.entrySet()) {
            RealtimePrincipal principal = entry.getValue().principal();
            if (Objects.equals(principal.tenantId(), tenantId)
                    && Objects.equals(principal.userId(), userId)
                    && sseConnections.remove(entry.getKey(), entry.getValue())) {
                entry.getValue().emitter().complete();
                disconnected++;
            }
        }
        for (Map.Entry<String, WebSocketConnection> entry : webSocketConnections.entrySet()) {
            RealtimePrincipal principal = entry.getValue().principal();
            if (Objects.equals(principal.tenantId(), tenantId)
                    && Objects.equals(principal.userId(), userId)
                    && webSocketConnections.remove(entry.getKey(), entry.getValue())) {
                try {
                    entry.getValue().session().close();
                } catch (IOException ignored) {
                    // 连接已经不可用，移除注册记录即可。
                }
                disconnected++;
            }
        }
        return disconnected;
    }


    private int publish(Predicate<RealtimePrincipal> predicate, RealtimeMessage message) {
        int delivered = 0;
        for (Map.Entry<String, SseConnection> entry : sseConnections.entrySet()) {
            if (predicate.test(entry.getValue().principal()) && sendSse(entry.getKey(), message)) {
                delivered++;
            }
        }
        for (Map.Entry<String, WebSocketConnection> entry : webSocketConnections.entrySet()) {
            if (predicate.test(entry.getValue().principal()) && sendWebSocket(entry.getKey(), message)) {
                delivered++;
            }
        }
        return delivered;
    }

    private boolean sendSse(String connectionId, RealtimeMessage message) {
        SseConnection connection = sseConnections.get(connectionId);
        if (connection == null) {
            return false;
        }
        try {
            connection.emitter().send(SseEmitter.event()
                    .id(message.id())
                    .name("message")
                    .data(message));
            return true;
        } catch (IOException | IllegalStateException exception) {
            sseConnections.remove(connectionId);
            connection.emitter().completeWithError(exception);
            return false;
        }
    }

    private boolean sendWebSocket(String connectionId, RealtimeMessage message) {
        WebSocketConnection connection = webSocketConnections.get(connectionId);
        if (connection == null || !connection.session().isOpen()) {
            webSocketConnections.remove(connectionId);
            return false;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            synchronized (connection.session()) {
                connection.session().sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException | IllegalStateException exception) {
            webSocketConnections.remove(connectionId);
            try {
                connection.session().close();
            } catch (IOException ignored) {
                // 连接已经不可用，无需再次抛出关闭异常。
            }
            return false;
        }
    }

    private record SseConnection(RealtimePrincipal principal, SseEmitter emitter) {
    }

    private record WebSocketConnection(RealtimePrincipal principal, WebSocketSession session) {
    }
}
