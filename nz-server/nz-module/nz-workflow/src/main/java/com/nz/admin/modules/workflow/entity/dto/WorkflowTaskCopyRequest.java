package com.nz.admin.modules.workflow.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 抄送任务参数。 */
@Data
public class WorkflowTaskCopyRequest {

    @Valid
    @NotEmpty(message = "抄送人不能为空")
    @Size(max = 50, message = "一次最多抄送50人")
    private List<@NotNull @Positive Long> receiverIds;

    @Size(max = 500, message = "抄送说明不能超过500个字符")
    private String comment;
}
