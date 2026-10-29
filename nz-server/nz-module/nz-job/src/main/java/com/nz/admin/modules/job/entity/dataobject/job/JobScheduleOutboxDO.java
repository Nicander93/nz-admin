package com.nz.admin.modules.job.entity.dataobject.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

/** 最新任务配置的调度同步状态；删除意图同样持久保留。 */
@Data
public class JobScheduleOutboxDO {
    private Long id;
    private String schedulerName;
    private Long tenantId;
    private Long jobId;
    private Long revision;
    private Long syncedRevision;
    private String operation;
    @JsonIgnore private String payload;
    private String state;
    @JsonIgnore private Long dispatchVersion;
    private Integer attempts;
    private LocalDateTime nextAttemptTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
