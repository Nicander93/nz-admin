package com.nz.admin.modules.system.entity.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeQuery extends PageQuery {

    private String name;
    private String type;
    private Integer status;
}
