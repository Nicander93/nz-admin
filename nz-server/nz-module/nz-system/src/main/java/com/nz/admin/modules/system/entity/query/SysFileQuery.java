package com.nz.admin.modules.system.entity.query;

import com.nz.admin.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysFileQuery extends PageQuery {

    private String originalName;

    private String fileExt;
}
