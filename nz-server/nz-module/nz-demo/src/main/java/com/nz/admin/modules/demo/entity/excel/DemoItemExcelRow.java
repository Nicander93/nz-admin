package com.nz.admin.modules.demo.entity.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/** 示例条目导入行。 */
@Data
public class DemoItemExcelRow {
    @ExcelProperty(value = "条目名称", index = 0)
    private String name;
    @ExcelProperty(value = "分类", index = 1)
    private String category;
    @ExcelProperty(value = "状态（0启用，1停用）", index = 2)
    private Integer status;
    @ExcelProperty(value = "排序（0-999）", index = 3)
    private Integer sort;
    @ExcelProperty(value = "备注", index = 4)
    private String remark;
}
