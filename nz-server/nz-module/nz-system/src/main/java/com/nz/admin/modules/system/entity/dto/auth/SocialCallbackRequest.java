package com.nz.admin.modules.system.entity.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** 第三方授权回调参数。 */
public record SocialCallbackRequest(
        @NotBlank(message = "服务商不能为空") String provider,
        @NotBlank(message = "授权码不能为空") String code,
        @NotBlank(message = "授权状态不能为空") String state
) {
}
