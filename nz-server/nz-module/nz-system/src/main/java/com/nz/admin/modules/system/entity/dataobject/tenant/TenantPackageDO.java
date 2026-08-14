package com.nz.admin.modules.system.entity.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 租户套餐实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_package")
public class TenantPackageDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String packageName;
    private Integer status;
    private String remark;
}
