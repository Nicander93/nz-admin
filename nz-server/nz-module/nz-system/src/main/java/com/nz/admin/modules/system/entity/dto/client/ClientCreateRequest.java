package com.nz.admin.modules.system.entity.dto.client;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建客户端请求。
 */
@Data
public class ClientCreateRequest {

    @NotBlank(message = "客户端标识不能为空")
    private String clientId;
    @NotBlank(message = "客户端名称不能为空")
    private String clientName;
    @NotBlank(message = "登录类型不能为空")
    private String loginType;
    @NotNull(message = "Token 有效期不能为空")
    @Min(value = 60, message = "Token 有效期不能小于 60 秒")
    @Max(value = 2592000, message = "Token 有效期不能超过 30 天")
    private Integer tokenTimeout;
    @NotNull(message = "状态不能为空")
    private Integer status;
    private String remark;
}
