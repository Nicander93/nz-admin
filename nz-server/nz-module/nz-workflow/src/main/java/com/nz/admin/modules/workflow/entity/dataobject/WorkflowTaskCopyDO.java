package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 流程任务抄送。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_task_copy")
public class WorkflowTaskCopyDO extends BaseEntity {

    @TableId(value = "copy_id", type = IdType.AUTO)
    private Long copyId;
    private Long tenantId;
    private Long taskId;
    private Long instanceId;
    private Long receiverId;
    private Long senderId;
    private String senderName;
    private String comment;
    private Integer readStatus;
    private LocalDateTime readTime;
}
