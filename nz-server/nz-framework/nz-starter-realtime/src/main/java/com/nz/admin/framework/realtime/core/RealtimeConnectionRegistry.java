package com.nz.admin.framework.realtime.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.common.core.identity.IdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/** 保存当前应用节点上的 SSE 与 WebSocket 连接。 */
public class RealtimeConnectionRegistry implements RealtimePublisher, RealtimeConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(RealtimeConnectionRegistry.class);

    private final ConcurrentMap<String, SseConnection> sseConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketConnection> webSocketConnections = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Duration sseTimeout;
    private final IdentityValidator validator;
    private final RealtimeAccessPolicy accessPolicy;

    /** 兼容手工创建的 standalone core，仅允许通用 scope。 */
    public RealtimeConnectionRegistry(ObjectMapper objectMapper, Duration sseTimeout) {
        this(objectMapper, sseTimeout, null, RealtimeAccessPolicy.generalOnly());
    }

    /** 兼容旧构造器，仅允许通用 scope。 */
    public RealtimeConnectionRegistry(
            ObjectMapper objectMapper,
            Duration sseTimeout,
            IdentityValidator validator) {
        this(objectMapper, sseTimeout, validator, RealtimeAccessPolicy.generalOnly());
    }

    public RealtimeConnectionRegistry(
            ObjectMapper objectMapper,
            Duration sseTimeout,
            IdentityValidator validator,
            RealtimeAccessPolicy accessPolicy) {
        this.objectMapper = objectMapper;
        this.sseTimeout = sseTimeout;
        this.validator = validator;
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
    }

    public SseEmitter connectSse(RealtimePrincipal principal) {
        if (!isAuthorized(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "实时连接身份或权限已失效");
        }
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(sseTimeout.toMillis());
        sseConnections.put(connectionId, new SseConnection(principal, emitter));
        emitter.onCompletion(() -> sseConnections.remove(connectionId));
        emitter.onTimeout(() -> {
            sseConnections.remove(connectionId);
            emitter.complete();
        });
        emitter.onError(error -> sseConnections.remove(connectionId));
        if (principal.scope() == RealtimeScope.GENERAL) {
            sendSse(connectionId, RealtimeDeliveryKind.USER, RealtimeMessage.of(
                    "connected",
                    Map.of("connectionId", connectionId, "transport", RealtimeTransport.SSE.name())
            ));
        } else {
            try {
                // 注释只完成 SSE 握手，不向通知通道放入诊断业务事件。
                emitter.send(SseEmitter.event().comment("connected"));
            } catch (IOException error) {
                sseConnections.remove(connectionId);
                emitter.completeWithError(error);
            }
        }
        return emitter;
    }

    public void connectWebSocket(WebSocketSession session, RealtimePrincipal principal) throws IOException {
        if (!isAuthorized(principal)) throw new IOException("实时连接身份或权限已失效");
        webSocketConnections.put(session.getId(), new WebSocketConnection(principal, session));
        if (principal.scope() == RealtimeScope.GENERAL
                && !sendWebSocket(session.getId(), RealtimeDeliveryKind.USER, RealtimeMessage.of(
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
        WebSocketConnection connection = webSocketConnections.get(connectionId);
        if (connection != null && connection.principal().scope() == RealtimeScope.GENERAL) {
            sendWebSocket(connectionId, RealtimeDeliveryKind.USER, RealtimeMessage.of("pong", Map.of()));
        }
    }

    @Override
    public int publishToUser(Long tenantId, Long userId, RealtimeMessage message) {
        return publish(
                RealtimeDeliveryKind.USER,
                principal -> Objects.equals(principal.tenantId(), tenantId)
                        && Objects.equals(principal.userId(), userId),
                message
        );
    }

    @Override
    public int publishToTenant(Long tenantId, RealtimeMessage message) {
        return publish(
                RealtimeDeliveryKind.TENANT,
                principal -> Objects.equals(principal.tenantId(), tenantId),
                message
        );
    }

    @Override
    public int broadcast(RealtimeMessage message) {
        return publish(RealtimeDeliveryKind.BROADCAST, principal -> true, message);
    }

    @Override
    public RealtimeConnectionStats stats() {
        return new RealtimeConnectionStats(sseConnections.size(), webSocketConnections.size());
    }

    @Override
    public int disconnectUser(Long tenantId, Long userId) {
        return disconnect(principal -> Objects.equals(principal.tenantId(), tenantId)
                && Objects.equals(principal.userId(), userId));
    }

    private int disconnect(Predicate<RealtimePrincipal> predicate) {
        int disconnected = 0;
        for (Map.Entry<String, SseConnection> entry : sseConnections.entrySet()) {
            RealtimePrincipal principal = entry.getValue().principal();
            if (predicate.test(principal)
                    && sseConnections.remove(entry.getKey(), entry.getValue())) {
                entry.getValue().emitter().complete();
                disconnected++;
            }
        }
        for (Map.Entry<String, WebSocketConnection> entry : webSocketConnections.entrySet()) {
            RealtimePrincipal principal = entry.getValue().principal();
            if (predicate.test(principal)
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

    /** 定期关闭身份或业务权限已撤销的连接。 */
    @Scheduled(fixedDelayString = "${nz.realtime.identity-check-interval-ms:1000}")
    public void disconnectInvalidIdentities() {
        // 只关闭本次检查前已经存在的连接；撤权查询返回时可能已恢复授权并新建连接。
        var sseSnapshot = Map.copyOf(sseConnections);
        var socketSnapshot = Map.copyOf(webSocketConnections);
        var authorized = new HashMap<RealtimePrincipal, Boolean>();
        sseSnapshot.forEach((id, connection) -> {
            if (!authorized.computeIfAbsent(connection.principal(), this::isAuthorized)) closeSse(id, connection);
        });
        socketSnapshot.forEach((id, connection) -> {
            if (!authorized.computeIfAbsent(connection.principal(), this::isAuthorized)) closeWebSocket(id, connection);
        });
    }

    private void closeSse(String id, SseConnection connection) {
        if (sseConnections.remove(id, connection)) connection.emitter().complete();
    }

    private void closeWebSocket(String id, WebSocketConnection connection) {
        if (webSocketConnections.remove(id, connection)) {
            try { connection.session().close(); }
            catch (IOException ignored) { }
        }
    }

    private boolean isAuthorized(RealtimePrincipal principal) {
        if (principal == null) return false;
        try {
            return (validator == null || validator.isValid(principal.identity()))
                    && accessPolicy.isAuthorized(principal);
        } catch (RuntimeException error) {
            log.warn("实时连接授权状态无法核对，关闭连接", error);
            return false;
        }
    }

    private boolean canReceive(
            RealtimePrincipal principal,
            RealtimeDeliveryKind deliveryKind,
            RealtimeMessage message,
            Runnable closeConnection) {
        try {
            return accessPolicy.canReceive(principal, deliveryKind, message);
        } catch (RuntimeException error) {
            log.warn("实时事件授权状态无法核对，关闭连接", error);
            closeConnection.run();
            return false;
        }
    }

    private int publish(
            RealtimeDeliveryKind deliveryKind,
            Predicate<RealtimePrincipal> predicate,
            RealtimeMessage message) {
        int delivered = 0;
        for (Map.Entry<String, SseConnection> entry : sseConnections.entrySet()) {
            if (predicate.test(entry.getValue().principal())
                    && sendSse(entry.getKey(), deliveryKind, message)) {
                delivered++;
            }
        }
        for (Map.Entry<String, WebSocketConnection> entry : webSocketConnections.entrySet()) {
            if (predicate.test(entry.getValue().principal())
                    && sendWebSocket(entry.getKey(), deliveryKind, message)) {
                delivered++;
            }
        }
        return delivered;
    }

    private boolean sendSse(
            String connectionId,
            RealtimeDeliveryKind deliveryKind,
            RealtimeMessage message) {
        SseConnection connection = sseConnections.get(connectionId);
        if (connection == null) return false;
        if (!isAuthorized(connection.principal())) {
            closeSse(connectionId, connection);
            return false;
        }
        if (!canReceive(connection.principal(), deliveryKind, message, () -> closeSse(connectionId, connection))) return false;
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

    private boolean sendWebSocket(
            String connectionId,
            RealtimeDeliveryKind deliveryKind,
            RealtimeMessage message) {
        WebSocketConnection connection = webSocketConnections.get(connectionId);
        if (connection == null || !connection.session().isOpen()) {
            webSocketConnections.remove(connectionId);
            return false;
        }
        if (!isAuthorized(connection.principal())) {
            closeWebSocket(connectionId, connection);
            return false;
        }
        if (!canReceive(connection.principal(), deliveryKind, message, () -> closeWebSocket(connectionId, connection))) return false;
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
