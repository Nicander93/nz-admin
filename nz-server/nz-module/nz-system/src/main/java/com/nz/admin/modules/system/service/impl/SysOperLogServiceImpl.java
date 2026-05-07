package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysOperLogDO;
import com.nz.admin.modules.system.mapper.SysOperLogMapper;
import com.nz.admin.modules.system.entity.query.SysOperLogQuery;
import com.nz.admin.modules.system.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现。
 */
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    @Autowired
    private SysOperLogMapper operLogMapper;

    /**
     * 按条件分页查操作日志。
     */
    @Override
    public Page<SysOperLogDO> listPage(SysOperLogQuery query) {
        return operLogMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查操作日志详情。
     */
    @Override
    public SysOperLogDO getById(Long id) {
        return operLogMapper.selectById(id);
    }

    /**
     * 批量删除操作日志。
     */
    @Override
    public void removeByIds(List<Long> ids) {
        operLogMapper.deleteBatchIds(ids);
    }

    /**
     * 异步写入操作日志。
     */
    @Async
    @Override
    public void saveAsync(SysOperLogDO operLog) {
        operLogMapper.insert(operLog);
    }
}
