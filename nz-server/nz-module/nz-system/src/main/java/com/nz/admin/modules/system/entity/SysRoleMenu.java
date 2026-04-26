package com.nz.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_role_menu")
public class SysRoleMenu {

    private Long roleId;
    private Long menuId;
}
