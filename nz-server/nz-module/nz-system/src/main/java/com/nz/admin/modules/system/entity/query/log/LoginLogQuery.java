package com.nz.admin.modules.system.entity.query.log;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends PageQuery {

    private String username;
    private String ip;
    private Integer status;
}
