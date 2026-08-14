package com.nz.admin.framework.realtime.core;

import java.time.Instant;
import java.util.UUID;

/**
 * SSE 与 WebSocket 共用的消息信封。
 */
public record RealtimeMessage(String id, String type, Instant sentAt, Object payload) {

    public static RealtimeMessage of(String type, Object payload) {
        return new RealtimeMessage(
                UUID.randomUUID().toString(),
                type,
                Instant.now(),
                payload
        );
    }
}
