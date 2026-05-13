package com.nz.admin.modules.system.entity.query.notice;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQuery extends PageQuery {

    private String title;
    private Integer type;
    private Integer status;
}
