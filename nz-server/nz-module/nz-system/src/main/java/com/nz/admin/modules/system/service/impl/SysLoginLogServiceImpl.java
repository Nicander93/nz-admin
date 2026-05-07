package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysLoginLogDO;
import com.nz.admin.modules.system.mapper.SysLoginLogMapper;
import com.nz.admin.modules.system.entity.query.SysLoginLogQuery;
import com.nz.admin.modules.system.service.SysLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现。
 */
@Service
public class SysLoginLogServiceImpl implements SysLoginLogService {

    @Autowired
    private SysLoginLogMapper loginLogMapper;

    /**
     * 按条件分页查登录日志。
     */
    @Override
    public Page<SysLoginLogDO> listPage(SysLoginLogQuery query) {
        return loginLogMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查登录日志详情。
     */
    @Override
    public SysLoginLogDO getById(Long id) {
        return loginLogMapper.selectById(id);
    }

    /**
     * 异步写入登录日志。
     */
    @Async
    @Override
    public void saveAsync(SysLoginLogDO loginLog) {
        loginLogMapper.insert(loginLog);
    }
}
