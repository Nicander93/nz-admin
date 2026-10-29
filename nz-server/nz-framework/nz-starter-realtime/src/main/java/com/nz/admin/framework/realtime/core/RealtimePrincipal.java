package com.nz.admin.framework.realtime.core;

import com.nz.admin.common.core.identity.IdentitySnapshot;

/** 建立连接时固定身份版本和订阅范围。 */
public record RealtimePrincipal(
        Long userId,
        Long tenantId,
        Long userVersion,
        Long tenantVersion,
        RealtimeScope scope) {

    public RealtimePrincipal {
        scope = scope == null ? RealtimeScope.GENERAL : scope;
    }

    public RealtimePrincipal(Long userId, Long tenantId, Long userVersion, Long tenantVersion) {
        this(userId, tenantId, userVersion, tenantVersion, RealtimeScope.GENERAL);
    }

    public RealtimePrincipal(Long userId, Long tenantId) {
        this(userId, tenantId, null, null, RealtimeScope.GENERAL);
    }

    public IdentitySnapshot identity() {
        return new IdentitySnapshot(userId, tenantId, userVersion, tenantVersion);
    }
}
