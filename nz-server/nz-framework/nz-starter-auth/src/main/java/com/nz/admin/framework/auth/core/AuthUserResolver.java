package com.nz.admin.framework.auth.core;

/**
 * 登录用户解析扩展点。
 */
@FunctionalInterface
public interface AuthUserResolver {

    LoginUser resolve(Long userId);
}
