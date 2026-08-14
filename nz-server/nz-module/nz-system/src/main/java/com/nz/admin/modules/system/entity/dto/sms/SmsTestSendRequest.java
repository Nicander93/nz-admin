package com.nz.admin.modules.system.entity.dto.sms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;

/** 测试发送短信请求。 */
@Data
public class SmsTestSendRequest {
    @NotNull(message = "短信模板不能为空")
    private Long templateId;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\+?[0-9]{6,20}$", message = "手机号格式不正确")
    private String phoneNumber;
    private Map<String, Object> parameters = new LinkedHashMap<>();
}
