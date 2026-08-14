package com.nz.admin.modules.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskDO;
import org.apache.ibatis.annotations.Mapper;

/** 当前任务 Mapper。 */
@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTaskDO> {
}
