package com.nz.admin.modules.system.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {

    private String name;
    private String roleKey;
    private Integer status;
}
