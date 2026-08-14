package com.nz.admin.framework.realtime.core;

/**
 * 建立实时连接时固化的服务端身份。
 *
 * @param userId 用户 ID
 * @param tenantId 租户 ID
 */
public record RealtimePrincipal(Long userId, Long tenantId) {
}
