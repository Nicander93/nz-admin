package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程实例审批动作。
 */
@Data
public class WorkflowInstanceActionRequest {

    @NotBlank(message = "审批动作不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "审批动作只支持 APPROVE 或 REJECT")
    private String action;

    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String comment;
}
