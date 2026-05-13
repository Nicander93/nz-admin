package com.nz.admin.modules.system.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.mapper.log.OperLogMapper;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现。
 */
@Service
public class OperLogServiceImpl implements OperLogService {

    @Autowired
    private OperLogMapper operLogMapper;

    /**
     * 按条件分页查操作日志。
     */
    @Override
    public Page<OperLogDO> listPage(OperLogQuery query) {
        return operLogMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查操作日志详情。
     */
    @Override
    public OperLogDO getById(Long id) {
        return operLogMapper.selectById(id);
    }

    /**
     * 批量删除操作日志。
     */
    @Override
    public void removeByIds(List<Long> ids) {
        operLogMapper.deleteBatchIds(ids);
    }

    @Override
    public int removeBefore(LocalDateTime deadline) {
        return operLogMapper.deleteBeforeOperTime(deadline);
    }

    /**
     * 异步写入操作日志。
     */
    @Async
    @Override
    public void saveAsync(OperLogDO operLog) {
        operLogMapper.insert(operLog);
    }
}
