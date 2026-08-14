package com.nz.admin.modules.system.entity.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 修改租户套餐请求。
 */
@Data
public class TenantPackageUpdateRequest {

    @NotNull
    private Long id;
    @NotBlank
    @Size(max = 128)
    private String packageName;
    @NotNull
    private Integer status;
    @Size(max = 500)
    private String remark;
    @NotNull
    private List<Long> menuIds;
}
