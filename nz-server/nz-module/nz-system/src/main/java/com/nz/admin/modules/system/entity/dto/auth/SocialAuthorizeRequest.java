package com.nz.admin.modules.system.entity.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** 发起第三方登录授权。 */
public record SocialAuthorizeRequest(
        @NotBlank(message = "租户编码不能为空") String tenantCode,
        @NotBlank(message = "客户端不能为空") String clientId,
        @NotBlank(message = "服务商不能为空") String provider
) {
}
