package com.nz.admin.framework.social.core;

/** 由业务层写入并随一次性 state 保存的授权上下文。 */
public record SocialAuthorizationContext(Long tenantId, String purpose, String clientId, Long userId) {
}
