package com.nz.admin.modules.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.workflow.entity.dataobject.WorkflowTaskCopyDO;
import org.apache.ibatis.annotations.Mapper;

/** 任务抄送 Mapper。 */
@Mapper
public interface WorkflowTaskCopyMapper extends BaseMapper<WorkflowTaskCopyDO> {
}
