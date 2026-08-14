package com.nz.admin.modules.workflow.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例视图。
 */
@Data
@Accessors(chain = true)
public class WorkflowInstanceVO {

    private Long instanceId;
    private Long definitionId;
    private String businessKey;
    private String title;
    private String flowCode;
    private String flowName;
    private Integer versionNo;
    private Long initiatorId;
    private String currentNodeId;
    private String currentNodeName;
    private String currentNodeType;
    private String currentAssignee;
    private String status;
    private Integer activityStatus;
    private String variablesJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime endTime;
    private List<WorkflowInstanceEventVO> events = new ArrayList<>();
}
