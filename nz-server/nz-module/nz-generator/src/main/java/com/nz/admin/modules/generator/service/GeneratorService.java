package com.nz.admin.modules.generator.service;

import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorPreview;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import com.nz.admin.modules.generator.model.GeneratorTable;

import java.util.List;

/**
 * 代码生成服务。
 */
public interface GeneratorService {

    List<GeneratorTable> listTables(String schemaName, String keyword);

    List<GeneratorColumn> listColumns(String schemaName, String tableName);

    GeneratorPreview preview(GeneratorRequest request);

    byte[] download(GeneratorRequest request);
}
