package com.nz.admin.framework.auth.core;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.common.core.identity.IdentitySnapshot;

/** 只从服务端 Token Session 读取身份，不接收客户端提交的版本。 */
public final class AuthSessionIdentity {
    public static final String USER_VERSION = "authUserVersion";
    public static final String TENANT_VERSION = "authTenantVersion";
    private AuthSessionIdentity() {}

    public static IdentitySnapshot current() {
        return read(StpUtil.getLoginIdAsLong(), StpUtil.getTokenSession());
    }

    public static IdentitySnapshot read(Long userId, SaSession session) {
        // tenantId 是 tenant starter 使用的服务端键；auth 不反向依赖 tenant。
        return new IdentitySnapshot(userId, session.has("tenantId") ? session.getLong("tenantId") : null,
                session.has(USER_VERSION) ? session.getLong(USER_VERSION) : null, session.has(TENANT_VERSION) ? session.getLong(TENANT_VERSION) : null);
    }
}
