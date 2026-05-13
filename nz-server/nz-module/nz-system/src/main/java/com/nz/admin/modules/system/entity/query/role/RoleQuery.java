package com.nz.admin.modules.system.entity.query.role;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery {

    private String name;
    private String roleKey;
    private Integer status;
    private Integer dataScope;
}
