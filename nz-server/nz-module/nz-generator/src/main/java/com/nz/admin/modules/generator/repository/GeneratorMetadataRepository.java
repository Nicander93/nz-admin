package com.nz.admin.modules.generator.repository;

import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorTable;

import java.util.List;

/**
 * 数据库结构读取边界。
 */
public interface GeneratorMetadataRepository {

    List<GeneratorTable> listTables(String schemaName, String keyword);

    List<GeneratorColumn> listColumns(String schemaName, String tableName);
}
