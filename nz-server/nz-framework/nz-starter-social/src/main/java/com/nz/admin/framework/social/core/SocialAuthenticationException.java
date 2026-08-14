package com.nz.admin.framework.social.core;

/** 第三方授权协议失败。 */
public class SocialAuthenticationException extends RuntimeException {
    public SocialAuthenticationException(String message) {
        super(message);
    }

    public SocialAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
