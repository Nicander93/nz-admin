package com.nz.admin.modules.system.entity.query.message;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 当前用户消息查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageQuery extends PageQuery {
    private String category;
    private String title;
    private Integer readStatus;
}
