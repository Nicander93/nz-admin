package com.nz.admin.modules.system.entity.dataobject.role;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("sys_role_dept")
public class RoleDeptDO {
    private Long tenantId;
    private Long roleId;
    private Long deptId;
}
