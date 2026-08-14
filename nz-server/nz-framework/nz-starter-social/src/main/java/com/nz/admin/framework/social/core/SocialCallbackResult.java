package com.nz.admin.framework.social.core;

/** 完成授权回调后的可信上下文与第三方身份。 */
public record SocialCallbackResult(SocialAuthorizationContext context, SocialIdentity identity) {
}
