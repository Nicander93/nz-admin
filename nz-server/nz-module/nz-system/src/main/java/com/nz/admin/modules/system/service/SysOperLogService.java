package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysOperLog;
import com.nz.admin.modules.system.query.SysOperLogQuery;

import java.util.List;

public interface SysOperLogService {

    Page<SysOperLog> listPage(SysOperLogQuery query);

    SysOperLog getById(Long id);

    void removeByIds(List<Long> ids);

    void saveAsync(SysOperLog operLog);
}
