package com.nz.admin.modules.job.entity.dto.job;

import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;

/** 写入字段白名单：客户端不能设置租户和审计列。 */
public record JobWriteRequest(
        @NotNull(groups = Update.class) @Positive Long id,
        @NotBlank @Size(max = 64) String jobName,
        @Size(max = 64) String jobGroup,
        @NotBlank @Size(max = 500) @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]*[.][a-zA-Z][a-zA-Z0-9_]*") String invokeTarget,
        @NotBlank @Size(max = 64) String cronExpression,
        @Min(0) @Max(1) Integer status,
        @Min(0) @Max(1) Integer concurrent,
        @Min(1) @Max(3) Integer misfirePolicy,
        @Size(max = 500) String remark) {
    public interface Update extends Default {}

    public JobDO toEntity() {
        return new JobDO().setId(id).setJobName(jobName).setJobGroup(jobGroup)
                .setInvokeTarget(invokeTarget).setCronExpression(cronExpression)
                .setStatus(status).setConcurrent(concurrent).setMisfirePolicy(misfirePolicy).setRemark(remark);
    }
}
