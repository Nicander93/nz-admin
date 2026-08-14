package com.nz.admin.modules.system.entity.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class TenantDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String contactUser;
    private String contactPhone;
    private Long packageId;
    private LocalDateTime expireTime;
    private Integer accountCount;
    private Integer status;
    private String remark;
}
