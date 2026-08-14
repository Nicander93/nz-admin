package com.nz.admin.modules.system.entity.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 当前用户修改个人资料请求。 */
public record ProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,
        @Email(message = "邮箱格式不正确")
        @Size(max = 320, message = "邮箱不能超过 320 个字符")
        String email,
        @Pattern(regexp = "^$|^\\+?[0-9]{6,20}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "性别不能为空")
        @Pattern(regexp = "0|1|2", message = "性别值无效")
        String gender
) {
}
