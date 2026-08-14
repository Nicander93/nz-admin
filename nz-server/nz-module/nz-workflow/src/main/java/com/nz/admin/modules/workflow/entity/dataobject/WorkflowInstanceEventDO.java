package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 流程实例流转事件。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_instance_event")
public class WorkflowInstanceEventDO extends BaseEntity {

    @TableId(value = "event_id", type = IdType.AUTO)
    private Long eventId;
    private Long tenantId;
    private Long instanceId;
    private String eventType;
    private String fromNodeId;
    private String fromNodeName;
    private String toNodeId;
    private String toNodeName;
    private Long operatorId;
    private String operatorName;
    private String comment;
}
