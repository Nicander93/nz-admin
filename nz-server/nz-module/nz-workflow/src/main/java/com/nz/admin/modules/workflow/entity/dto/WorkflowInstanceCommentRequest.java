package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 撤回或终止流程参数。
 */
@Data
public class WorkflowInstanceCommentRequest {

    @Size(max = 500, message = "操作说明不能超过500个字符")
    private String comment;
}
