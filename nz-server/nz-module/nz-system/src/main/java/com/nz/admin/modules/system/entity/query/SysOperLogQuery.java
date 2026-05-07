package com.nz.admin.modules.system.entity.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperLogQuery extends PageQuery {

    private String title;
    private String operName;
    private Integer businessType;
    private Integer status;
}
