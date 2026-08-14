package com.nz.admin.modules.system.entity.vo.tenant;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户套餐详情。
 */
@Data
public class TenantPackageVO {

    private Long id;
    private String packageName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Long> menuIds;
}
