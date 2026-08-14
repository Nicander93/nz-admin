package com.nz.admin.modules.generator.model;

import lombok.Data;

/**
 * 数据库列元数据。
 */
@Data
public class GeneratorColumn {

    private Integer ordinalPosition;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String udtName;
    private Boolean nullable;
    private String defaultValue;
    private Boolean primaryKey;
    private Boolean identity;
}
