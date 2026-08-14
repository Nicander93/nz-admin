package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发起流程实例参数。
 */
@Data
public class WorkflowInstanceStartRequest {

    @NotBlank(message = "流程编码不能为空")
    private String flowCode;

    @NotBlank(message = "业务标识不能为空")
    @Size(max = 100, message = "业务标识不能超过100个字符")
    private String businessKey;

    @NotBlank(message = "流程标题不能为空")
    @Size(max = 200, message = "流程标题不能超过200个字符")
    private String title;

    private Map<String, Object> variables = new LinkedHashMap<>();
}
