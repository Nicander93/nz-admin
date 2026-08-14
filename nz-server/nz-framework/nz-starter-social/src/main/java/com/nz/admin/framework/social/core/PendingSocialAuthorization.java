package com.nz.admin.framework.social.core;

import java.time.Instant;

/** 服务端保存的待完成授权。 */
public record PendingSocialAuthorization(String provider, SocialAuthorizationContext context,
                                         String codeVerifier, String redirectUri, Instant expiresAt) {
}
