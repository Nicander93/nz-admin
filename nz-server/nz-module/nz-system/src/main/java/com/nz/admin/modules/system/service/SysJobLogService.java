package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJobLog;

public interface SysJobLogService {

    Page<SysJobLog> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    SysJobLog getById(Long id);

    void removeByIds(java.util.List<Long> ids);

    void save(SysJobLog jobLog);
}
