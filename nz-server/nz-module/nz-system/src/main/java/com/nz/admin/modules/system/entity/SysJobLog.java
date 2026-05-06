package com.nz.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 定时任务日志实体。
 */
@Data
@Accessors(chain = true)
@TableName("sys_job_log")
public class SysJobLog {

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
     * 调用目标。
     */
    private String invokeTarget;
    /**
     * 日志信息。
     */
    private String jobMessage;
    /**
     * 执行状态：0=成功，1=失败。
     */
    private Integer status;
    /**
     * 异常信息。
     */
    private String exceptionInfo;
    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}
