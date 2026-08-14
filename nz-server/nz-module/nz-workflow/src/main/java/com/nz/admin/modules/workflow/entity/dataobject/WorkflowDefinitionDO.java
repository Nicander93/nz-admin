package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 流程定义版本。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_definition")
public class WorkflowDefinitionDO extends BaseEntity {

    @TableId(value = "definition_id", type = IdType.AUTO)
    private Long definitionId;
    private Long tenantId;
    private String flowCode;
    private String flowName;
    private Long categoryId;
    private Integer versionNo;
    /** 发布状态：0 草稿，1 已发布，9 已失效。 */
    private Integer publishStatus;
    /** 运行状态：0 挂起，1 激活。 */
    private Integer activityStatus;
    private String formPath;
    private String modelJson;
    private String remark;
}
