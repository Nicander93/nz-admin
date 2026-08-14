package com.nz.admin.framework.social.core;

import java.time.Instant;

/** 发起第三方授权所需的跳转信息。 */
public record SocialAuthorization(String authorizeUrl, String state, Instant expiresAt) {
}
