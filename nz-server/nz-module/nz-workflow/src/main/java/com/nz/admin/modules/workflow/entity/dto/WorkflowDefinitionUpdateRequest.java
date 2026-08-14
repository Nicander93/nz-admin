package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改流程定义参数。
 */
@Data
public class WorkflowDefinitionUpdateRequest {

    @NotNull(message = "流程定义编号不能为空")
    private Long definitionId;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称不能超过100个字符")
    private String flowName;

    @NotNull(message = "流程分类不能为空")
    private Long categoryId;

    @Size(max = 200, message = "表单路径不能超过200个字符")
    private String formPath;

    @NotBlank(message = "流程模型不能为空")
    private String modelJson;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
