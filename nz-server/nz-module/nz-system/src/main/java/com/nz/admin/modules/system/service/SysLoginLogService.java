package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysLoginLogDO;
import com.nz.admin.modules.system.entity.query.SysLoginLogQuery;

public interface SysLoginLogService {

    Page<SysLoginLogDO> listPage(SysLoginLogQuery query);

    SysLoginLogDO getById(Long id);

    void saveAsync(SysLoginLogDO loginLog);
}
