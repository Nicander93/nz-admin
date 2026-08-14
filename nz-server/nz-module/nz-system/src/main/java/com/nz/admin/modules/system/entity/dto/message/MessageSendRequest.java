package com.nz.admin.modules.system.entity.dto.message;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理员发送站内消息。 */
public record MessageSendRequest(
        @NotBlank(message = "消息分类不能为空")
        @Pattern(regexp = "system|notice|workflow", message = "消息分类无效")
        String category,
        @Size(max = 32, message = "消息类型不能超过 32 个字符")
        String type,
        @Size(max = 64, message = "消息来源不能超过 64 个字符")
        String source,
        @NotBlank(message = "消息标题不能为空")
        @Size(max = 200, message = "消息标题不能超过 200 个字符")
        String title,
        @Size(max = 500, message = "消息摘要不能超过 500 个字符")
        String summary,
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 10000, message = "消息内容不能超过 10000 个字符")
        String content,
        @Size(max = 4000, message = "扩展数据不能超过 4000 个字符")
        String dataJson,
        @Size(max = 500, message = "跳转路径不能超过 500 个字符")
        String path,
        @NotBlank(message = "接收范围不能为空")
        @Pattern(regexp = "ALL|USERS", message = "接收范围无效")
        String targetType,
        @Valid
        @Size(max = 500, message = "单次最多选择 500 个用户")
        List<@NotNull(message = "用户 ID 不能为空") Long> userIds
) {
}
