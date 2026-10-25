package com.nz.admin.framework.realtime.core;

import com.nz.admin.common.core.identity.IdentitySnapshot;

/** 建立连接时固定身份版本；旧构造器供未装配身份校验的独立 starter 使用。 */
public record RealtimePrincipal(Long userId, Long tenantId, Long userVersion, Long tenantVersion) {
    public RealtimePrincipal(Long userId, Long tenantId) {
        this(userId, tenantId, null, null);
    }

    public IdentitySnapshot identity() {
        return new IdentitySnapshot(userId, tenantId, userVersion, tenantVersion);
    }
}
