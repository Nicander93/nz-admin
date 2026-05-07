package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysOperLogDO;
import com.nz.admin.modules.system.entity.query.SysOperLogQuery;

import java.util.List;

public interface SysOperLogService {

    Page<SysOperLogDO> listPage(SysOperLogQuery query);

    SysOperLogDO getById(Long id);

    void removeByIds(List<Long> ids);

    void saveAsync(SysOperLogDO operLog);
}
