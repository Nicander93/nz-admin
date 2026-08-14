package com.nz.admin.modules.workflow.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 流程分类导出行。
 */
@Data
public class WorkflowCategoryExportVO {

    @ExcelProperty("分类编号")
    private Long categoryId;

    @ExcelProperty("上级分类编号")
    private Long parentId;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("显示顺序")
    private Integer orderNum;
}
