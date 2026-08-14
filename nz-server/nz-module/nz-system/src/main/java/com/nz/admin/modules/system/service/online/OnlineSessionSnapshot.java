package com.nz.admin.modules.system.service.online;

import java.time.LocalDateTime;

/**
 * Sa-Token 会话读取结果。
 */
public record OnlineSessionSnapshot(
        String tokenValue,
        Long userId,
        Long tenantId,
        String tenantCode,
        String username,
        String deptName,
        String loginIp,
        LocalDateTime loginTime,
        String userAgent,
        long tokenTimeout
) {
}
