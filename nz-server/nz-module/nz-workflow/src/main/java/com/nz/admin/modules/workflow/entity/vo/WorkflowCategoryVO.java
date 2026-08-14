package com.nz.admin.modules.workflow.entity.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程分类视图。
 */
@Data
@Accessors(chain = true)
public class WorkflowCategoryVO {

    private Long categoryId;
    private Long parentId;
    private String ancestors;
    private String categoryName;
    private Integer orderNum;
    private Integer builtIn;
    private LocalDateTime createTime;
    private List<WorkflowCategoryVO> children = new ArrayList<>();
}
