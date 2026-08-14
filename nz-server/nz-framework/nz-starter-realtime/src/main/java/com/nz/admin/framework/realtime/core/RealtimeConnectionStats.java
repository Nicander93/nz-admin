package com.nz.admin.framework.realtime.core;

/**
 * 当前节点的实时连接统计。
 */
public record RealtimeConnectionStats(
        int sseConnections,
        int webSocketConnections,
        int totalConnections) {
    public RealtimeConnectionStats(int sseConnections, int webSocketConnections) {
        this(sseConnections, webSocketConnections, sseConnections + webSocketConnections);
    }
}
