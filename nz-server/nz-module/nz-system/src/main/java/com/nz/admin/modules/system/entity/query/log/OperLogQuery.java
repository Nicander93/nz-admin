package com.nz.admin.modules.system.entity.query.log;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogQuery extends PageQuery {

    private String title;
    private String operName;
    private Integer businessType;
    private Integer status;
}
