package com.nz.admin.modules.system.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.mapper.log.LoginLogMapper;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志服务实现。
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * 按条件分页查登录日志。
     */
    @Override
    public Page<LoginLogDO> listPage(LoginLogQuery query) {
        return loginLogMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查登录日志详情。
     */
    @Override
    public LoginLogDO getById(Long id) {
        return loginLogMapper.selectById(id);
    }

    /**
     * 异步写入登录日志。
     */
    @Async
    @Override
    public void saveAsync(LoginLogDO loginLog) {
        loginLogMapper.insert(loginLog);
    }

    @Override
    public void removeByIds(List<Long> ids) {
        loginLogMapper.deleteBatchIds(ids);
    }

    @Override
    public int removeBefore(LocalDateTime deadline) {
        return loginLogMapper.deleteBeforeLoginTime(deadline);
    }
}
