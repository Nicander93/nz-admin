package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysJobLogDO;

public interface SysJobLogService {

    Page<SysJobLogDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    SysJobLogDO getById(Long id);

    void removeByIds(java.util.List<Long> ids);

    void save(SysJobLogDO jobLog);
}
