package com.nz.admin.framework.realtime.core;

/**
 * 业务模块使用的实时消息发布端口。
 */
public interface RealtimePublisher {

    int publishToUser(Long tenantId, Long userId, RealtimeMessage message);

    int publishToTenant(Long tenantId, RealtimeMessage message);

    int broadcast(RealtimeMessage message);

    RealtimeConnectionStats stats();
}
