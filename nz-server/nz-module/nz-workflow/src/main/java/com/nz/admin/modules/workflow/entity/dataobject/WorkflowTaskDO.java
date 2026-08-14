package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** 当前待办任务。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_task")
public class WorkflowTaskDO extends BaseEntity {

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;
    private Long tenantId;
    private Long definitionId;
    private Long instanceId;
    private String nodeId;
    private String nodeName;
    private String assignee;
    private Long assigneeUserId;
    private String ownerAssignee;
    private Long ownerUserId;
    /** 0 普通任务，1 已委派给受托人。 */
    private Integer delegationStatus;
}
