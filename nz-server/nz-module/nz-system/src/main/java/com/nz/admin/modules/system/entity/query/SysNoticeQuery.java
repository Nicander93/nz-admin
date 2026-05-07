package com.nz.admin.modules.system.entity.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysNoticeQuery extends PageQuery {

    private String title;
    private Integer type;
    private Integer status;
}
