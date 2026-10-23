package com.nz.admin.modules.demo.entity.vo;

import com.nz.admin.framework.excel.core.ExcelImportError;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 导入预检结果；rows 只包含字段已标准化的有效行。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoItemImportResult {
    private int total;
    private int validCount;
    private List<DemoItemCreateRequest> rows;
    private List<ExcelImportError> errors;
}
