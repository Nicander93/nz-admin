package com.nz.admin.modules.workflow.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 流程任务视图。 */
@Data
@Accessors(chain = true)
public class WorkflowTaskVO {

    private Long taskId;
    private Long historyId;
    private Long copyId;
    private Long instanceId;
    private Long definitionId;
    private String businessKey;
    private String title;
    private String flowCode;
    private String flowName;
    private Integer versionNo;
    private String nodeId;
    private String nodeName;
    private String assignee;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String targetNodeName;
    private String targetAssignee;
    private String ownerAssignee;
    private Integer delegationStatus;
    private String comment;
    private Integer readStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
