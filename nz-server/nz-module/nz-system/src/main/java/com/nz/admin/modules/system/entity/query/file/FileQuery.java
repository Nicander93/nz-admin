package com.nz.admin.modules.system.entity.query.file;

import com.nz.admin.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileQuery extends PageQuery {

    private String originalName;

    private String fileExt;
}
