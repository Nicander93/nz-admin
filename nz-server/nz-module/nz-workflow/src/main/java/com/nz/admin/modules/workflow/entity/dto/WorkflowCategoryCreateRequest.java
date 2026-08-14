package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增流程分类参数。
 */
@Data
public class WorkflowCategoryCreateRequest {

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
