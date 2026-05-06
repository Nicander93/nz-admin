package com.nz.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 定时任务实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 任务名称。
     */
    private String jobName;
    /**
     * 任务分组。
     */
    private String jobGroup;
    /**
     * 调用目标：beanName.methodName 格式。
     */
    private String invokeTarget;
    /**
     * Cron 表达式。
     */
    private String cronExpression;
    /**
     * 状态：0=正常，1=暂停。
     */
    private Integer status;
    /**
     * 并发策略：0=允许，1=禁止。
     */
    private Integer concurrent;
    /**
     * 备注。
     */
    private String remark;
}
