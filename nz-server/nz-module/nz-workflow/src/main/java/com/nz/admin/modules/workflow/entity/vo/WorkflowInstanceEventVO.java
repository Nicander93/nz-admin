package com.nz.admin.modules.workflow.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 流程实例事件视图。
 */
@Data
@Accessors(chain = true)
public class WorkflowInstanceEventVO {

    private Long eventId;
    private String eventType;
    private String fromNodeId;
    private String fromNodeName;
    private String toNodeId;
    private String toNodeName;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private LocalDateTime createTime;
}
