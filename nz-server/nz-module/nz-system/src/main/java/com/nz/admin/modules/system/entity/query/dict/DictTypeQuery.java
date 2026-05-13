package com.nz.admin.modules.system.entity.query.dict;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQuery extends PageQuery {

    private String name;
    private String type;
    private Integer status;
}
