package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysLoginLog;
import com.nz.admin.modules.system.query.SysLoginLogQuery;

public interface SysLoginLogService {

    Page<SysLoginLog> listPage(SysLoginLogQuery query);

    SysLoginLog getById(Long id);

    void saveAsync(SysLoginLog loginLog);
}
