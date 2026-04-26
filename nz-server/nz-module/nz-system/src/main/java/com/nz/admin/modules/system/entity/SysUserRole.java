package com.nz.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_user_role")
public class SysUserRole {

    private Long userId;
    private Long roleId;
}
