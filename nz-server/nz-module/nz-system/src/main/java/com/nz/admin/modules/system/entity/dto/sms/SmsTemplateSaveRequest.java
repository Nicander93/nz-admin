package com.nz.admin.modules.system.entity.dto.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 保存短信模板请求。 */
@Data
public class SmsTemplateSaveRequest {
    private Long id;
    @NotNull(message = "短信渠道不能为空")
    private Long channelId;
    @NotBlank(message = "模板编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "模板编码格式不正确")
    private String templateCode;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String providerTemplateId;
    @NotBlank(message = "模板内容不能为空")
    @Size(max = 1000, message = "模板内容不能超过 1000 个字符")
    private String content;
    @NotNull(message = "状态不能为空")
    private Integer status;
    private String remark;
}
