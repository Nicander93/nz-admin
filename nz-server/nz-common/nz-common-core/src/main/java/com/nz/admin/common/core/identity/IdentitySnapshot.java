package com.nz.admin.common.core.identity;

/** 登录或实时连接建立时固定的服务端身份版本。 */
public record IdentitySnapshot(Long userId, Long tenantId, Long userVersion, Long tenantVersion) {
    public boolean isComplete() {
        return userId != null && tenantId != null && userVersion != null && tenantVersion != null
                && userVersion >= 0 && tenantVersion >= 0;
    }
}
