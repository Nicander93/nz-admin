package com.nz.admin.modules.system.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_user_role")
public class SysUserRoleDO {

    private Long userId;
    private Long roleId;
}
