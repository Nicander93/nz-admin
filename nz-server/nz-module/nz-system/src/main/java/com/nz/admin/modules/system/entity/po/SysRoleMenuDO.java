package com.nz.admin.modules.system.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_role_menu")
public class SysRoleMenuDO {

    private Long roleId;
    private Long menuId;
}
