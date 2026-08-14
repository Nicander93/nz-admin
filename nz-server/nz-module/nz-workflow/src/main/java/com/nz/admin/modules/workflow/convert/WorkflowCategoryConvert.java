package com.nz.admin.modules.workflow.convert;

import com.nz.admin.modules.workflow.entity.dataobject.WorkflowCategoryDO;
import com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryExportVO;
import com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryVO;

import java.util.List;

/**
 * 流程分类对象转换。
 */
public final class WorkflowCategoryConvert {

    private WorkflowCategoryConvert() {
    }

    public static WorkflowCategoryVO toVO(WorkflowCategoryDO category) {
        return new WorkflowCategoryVO()
                .setCategoryId(category.getCategoryId())
                .setParentId(category.getParentId())
                .setAncestors(category.getAncestors())
                .setCategoryName(category.getCategoryName())
                .setOrderNum(category.getOrderNum())
                .setBuiltIn(category.getBuiltIn())
                .setCreateTime(category.getCreateTime());
    }

    public static List<WorkflowCategoryExportVO> toExportList(List<WorkflowCategoryDO> categories) {
        return categories.stream().map(category -> {
            WorkflowCategoryExportVO row = new WorkflowCategoryExportVO();
            row.setCategoryId(category.getCategoryId());
            row.setParentId(category.getParentId());
            row.setCategoryName(category.getCategoryName());
            row.setOrderNum(category.getOrderNum());
            return row;
        }).toList();
    }
}
