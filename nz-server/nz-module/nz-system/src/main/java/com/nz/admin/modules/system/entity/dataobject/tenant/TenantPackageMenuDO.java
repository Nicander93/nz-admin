package com.nz.admin.modules.system.entity.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 租户套餐菜单关系。
 */
@Data
@Accessors(chain = true)
@TableName("sys_tenant_package_menu")
public class TenantPackageMenuDO {

    private Long packageId;
    private Long menuId;
}
