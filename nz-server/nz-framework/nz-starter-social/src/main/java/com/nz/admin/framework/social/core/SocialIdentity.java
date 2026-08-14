package com.nz.admin.framework.social.core;

/** 第三方服务商返回的稳定身份和公开资料。 */
public record SocialIdentity(String provider, String providerUserId, String username,
                             String nickname, String email, String avatar) {
}
