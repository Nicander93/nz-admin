package com.nz.admin.framework.excel.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入错误行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportError {

    private Integer rowNum;
    private String message;
}
