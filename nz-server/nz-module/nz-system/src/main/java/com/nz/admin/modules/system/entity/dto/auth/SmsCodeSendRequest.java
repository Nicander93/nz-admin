package com.nz.admin.modules.system.entity.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 发送登录短信验证码请求。 */
public record SmsCodeSendRequest(
        @NotBlank(message = "租户编码不能为空")
        String tenantCode,
        @NotBlank(message = "客户端标识不能为空")
        String clientId,
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "手机号格式不正确")
        String phone
) {
}
