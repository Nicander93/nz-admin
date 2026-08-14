package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 委派任务参数。 */
@Data
public class WorkflowTaskDelegateRequest {

    @NotNull(message = "受托用户不能为空")
    @Positive(message = "受托用户不正确")
    private Long targetUserId;

    @Size(max = 500, message = "委派说明不能超过500个字符")
    private String comment;
}
