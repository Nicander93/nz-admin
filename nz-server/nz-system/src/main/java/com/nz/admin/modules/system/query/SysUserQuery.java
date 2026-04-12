package com.nz.admin.modules.system.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {

    private String username;
    private String nickname;
    private Integer status;
}
