package com.nz.admin.modules.generator.model;

import lombok.Data;

/**
 * 可生成的数据库表。
 */
@Data
public class GeneratorTable {

    private String schemaName;
    private String tableName;
    private String tableComment;
    private Integer columnCount;
}
