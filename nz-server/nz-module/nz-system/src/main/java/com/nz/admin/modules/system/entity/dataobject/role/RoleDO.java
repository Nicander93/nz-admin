package com.nz.admin.modules.system.entity.dataobject.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import com.nz.admin.framework.datascope.DataScopeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class RoleDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String roleKey;
    private Integer sort;
    private Integer status;
    private String remark;
    
    /** 数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限） */
    private Integer dataScope;
}
