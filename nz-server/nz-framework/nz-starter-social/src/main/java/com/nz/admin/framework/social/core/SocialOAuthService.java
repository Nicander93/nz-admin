package com.nz.admin.framework.social.core;

import java.util.List;

/** 通用 OAuth2/OIDC 授权协议。 */
public interface SocialOAuthService {
    List<SocialProvider> providers();

    SocialAuthorization authorize(String provider, SocialAuthorizationContext context);

    SocialCallbackResult callback(String provider, String code, String state);
}
