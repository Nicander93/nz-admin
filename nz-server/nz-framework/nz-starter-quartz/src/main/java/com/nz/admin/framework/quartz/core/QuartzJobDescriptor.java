package com.nz.admin.framework.quartz.core;

import lombok.Data;

/**
 * Quartz 任务描述对象。
 */
@Data
public class QuartzJobDescriptor {

    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer status;
    private Integer concurrent;
    private Integer misfirePolicy;
}
