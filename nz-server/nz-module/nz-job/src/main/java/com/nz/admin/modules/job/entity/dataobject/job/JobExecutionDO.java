package com.nz.admin.modules.job.entity.dataobject.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** 持久逻辑执行和恢复状态，不向客户端暴露内部所有权令牌。 */
@Data
@Accessors(chain = true)
public class JobExecutionDO {
    private Long id;
    private String schedulerName;
    private Long tenantId;
    private Long jobId;
    private Long definitionRevision;
    private String executionKey;
    private String triggerSource;
    private Long scheduledFireTime;
    @JsonIgnore private String snapshot;
    private Boolean recoverable;
    /** 当前定义仍支持重试；调用方另需任务修改权限。 */
    private boolean retryable;
    private String state;
    @JsonIgnore private Long dispatchVersion;
    @JsonIgnore private Boolean dispatched;
    private Integer dispatchAttempts;
    private LocalDateTime nextAttemptTime;
    private Integer attempt;
    @JsonIgnore private String ownerToken;
    private String nodeId;
    private Boolean recovered;
    private LocalDateTime leaseUntil;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
