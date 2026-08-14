package com.nz.admin.modules.workflow.entity.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 流程分类。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("flow_category")
public class WorkflowCategoryDO extends BaseEntity {

    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;
    private Long tenantId;
    private Long parentId;
    private String ancestors;
    private String categoryName;
    private Integer orderNum;
    /** 是否为内置分类：0 否，1 是。 */
    private Integer builtIn;
}
