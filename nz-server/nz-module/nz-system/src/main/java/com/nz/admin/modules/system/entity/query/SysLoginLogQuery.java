package com.nz.admin.modules.system.entity.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysLoginLogQuery extends PageQuery {

    private String username;
    private String ip;
    private Integer status;
}
