package com.nz.admin.common.job;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 任务调用写日志用的事件数据。
 */
@Data
@Accessors(chain = true)
public class JobInvokeLogEvent {

    private String invokeTarget;
    private Integer status;
    private String jobMessage;
    private String exceptionInfo;
    private LocalDateTime createTime;
}
