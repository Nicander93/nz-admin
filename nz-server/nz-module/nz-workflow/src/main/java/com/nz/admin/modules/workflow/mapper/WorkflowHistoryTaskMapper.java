package com.nz.admin.modules.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowHistoryTaskDO;
import org.apache.ibatis.annotations.Mapper;

/** 历史任务 Mapper。 */
@Mapper
public interface WorkflowHistoryTaskMapper extends BaseMapper<WorkflowHistoryTaskDO> {
}
