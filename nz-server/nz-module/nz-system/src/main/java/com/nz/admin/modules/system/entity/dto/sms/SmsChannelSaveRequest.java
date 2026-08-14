package com.nz.admin.modules.system.entity.dto.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 保存短信渠道请求。 */
@Data
public class SmsChannelSaveRequest {
    private Long id;
    @NotBlank(message = "渠道编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "渠道编码只能包含字母、数字、下划线和短横线")
    private String channelCode;
    @NotBlank(message = "渠道名称不能为空")
    private String channelName;
    @NotBlank(message = "供应商编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "供应商编码格式不正确")
    private String providerCode;
    @Size(max = 500, message = "Webhook 地址不能超过 500 个字符")
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String signature;
    @NotNull(message = "状态不能为空")
    private Integer status;
    private String remark;
}
