package com.nz.admin.framework.realtime.core;

/**
 * 关闭指定用户在当前节点上的实时连接。
 */
public interface RealtimeConnectionManager {

    int disconnectUser(Long tenantId, Long userId);
}
