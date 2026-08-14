package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 流程实例。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_instance")
public class WorkflowInstanceDO extends BaseEntity {

    @TableId(value = "instance_id", type = IdType.AUTO)
    private Long instanceId;
    private Long tenantId;
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
    /** RUNNING、COMPLETED、REJECTED、CANCELED、TERMINATED。 */
    private String status;
    /** 运行状态：0 挂起，1 激活。 */
    private Integer activityStatus;
    private String variablesJson;
    private String modelJson;
    private LocalDateTime endTime;
}
