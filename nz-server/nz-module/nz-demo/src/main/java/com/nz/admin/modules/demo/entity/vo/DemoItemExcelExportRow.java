package com.nz.admin.modules.demo.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 条目导出的业务字段，不包含数据库归属列。 */
@Data
public class DemoItemExcelExportRow {
    @ExcelProperty("条目名称")
    private String name;
    @ExcelProperty("分类")
    private String category;
    @ExcelProperty("状态（0启用，1停用）")
    private Integer status;
    @ExcelProperty("排序")
    private Integer sort;
    @ExcelProperty("备注")
    private String remark;
}
