package com.nz.admin.modules.system.entity.dto.tenant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增租户请求。
 */
@Data
public class TenantCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String tenantCode;
    @NotBlank
    @Size(max = 128)
    private String tenantName;
    @Size(max = 64)
    private String contactUser;
    @Size(max = 20)
    private String contactPhone;
    @NotNull
    private Long packageId;
    private LocalDateTime expireTime;
    @NotNull
    @Min(1)
    private Integer accountCount;
    @NotNull
    private Integer status;
    @Size(max = 500)
    private String remark;
    @NotBlank
    @Size(max = 64)
    private String adminUsername;
    @NotBlank
    @Size(min = 6, max = 64)
    private String adminPassword;
}
