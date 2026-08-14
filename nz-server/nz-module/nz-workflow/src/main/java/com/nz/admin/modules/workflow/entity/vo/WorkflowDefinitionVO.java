package com.nz.admin.modules.workflow.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 流程定义视图。
 */
@Data
@Accessors(chain = true)
public class WorkflowDefinitionVO {

    private Long definitionId;
    private String flowCode;
    private String flowName;
    private Long categoryId;
    private String categoryName;
    private Integer versionNo;
    private Integer publishStatus;
    private Integer activityStatus;
    private String formPath;
    private String modelJson;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
