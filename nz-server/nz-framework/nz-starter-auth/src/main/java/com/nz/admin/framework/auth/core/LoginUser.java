package com.nz.admin.framework.auth.core;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 轻量登录用户上下文。
 */
@Data
public class LoginUser {

    private Long userId;
    private String username;
    private Set<String> permissions = new LinkedHashSet<>();
    private Set<String> roles = new LinkedHashSet<>();
}
