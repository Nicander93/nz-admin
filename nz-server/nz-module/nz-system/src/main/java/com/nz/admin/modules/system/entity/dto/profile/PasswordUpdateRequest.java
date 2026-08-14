package com.nz.admin.modules.system.entity.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 当前用户修改密码请求。 */
public record PasswordUpdateRequest(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "新密码长度必须在 6 到 64 个字符之间")
        String newPassword
) {
}
