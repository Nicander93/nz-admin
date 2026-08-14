package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 已办任务记录。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_history_task")
public class WorkflowHistoryTaskDO extends BaseEntity {

    @TableId(value = "history_id", type = IdType.AUTO)
    private Long historyId;
    private Long tenantId;
    private Long taskId;
    private Long definitionId;
    private Long instanceId;
    private String nodeId;
    private String nodeName;
    private String assignee;
    private Long operatorId;
    private String operatorName;
    /** APPROVE、REJECT、TRANSFER、DELEGATE、RESOLVE、CANCEL、TERMINATE。 */
    private String action;
    private String targetNodeId;
    private String targetNodeName;
    private String targetAssignee;
    private String comment;
    private LocalDateTime taskCreateTime;
}
