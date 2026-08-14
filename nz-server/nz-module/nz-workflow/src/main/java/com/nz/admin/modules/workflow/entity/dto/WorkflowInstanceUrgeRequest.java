package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程实例催办参数。
 */
@Data
public class WorkflowInstanceUrgeRequest {

    @NotBlank(message = "催办内容不能为空")
    @Size(max = 500, message = "催办内容不能超过500个字符")
    private String content;
}
