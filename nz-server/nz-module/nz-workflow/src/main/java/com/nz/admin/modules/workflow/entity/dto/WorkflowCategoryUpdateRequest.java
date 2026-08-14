package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改流程分类参数。
 */
@Data
public class WorkflowCategoryUpdateRequest {

    @NotNull(message = "分类编号不能为空")
    private Long categoryId;

    @NotNull(message = "上级分类不能为空")
    @Min(value = 0, message = "上级分类不正确")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称不能超过30个字符")
    private String categoryName;

    @NotNull(message = "显示顺序不能为空")
    @Min(value = 0, message = "显示顺序不能小于0")
    private Integer orderNum;
}
