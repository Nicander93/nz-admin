package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.framework.auth.datascope.DataScope;
import com.nz.admin.framework.auth.datascope.DataScopeType;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.mapper.SysUserMapper;
import com.nz.admin.modules.system.entity.query.SysUserQuery;
import com.nz.admin.modules.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户这块的服务实现。
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 按分页条件查用户列表。
     */
    @DataScope(value = {DataScopeType.ALL, DataScopeType.DEPT, DataScopeType.SELF}, deptAlias = "dept_id", userAlias = "id")
    @Override
    public Page<SysUserDO> listPage(SysUserQuery query) {
        return userMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿用户详情。
     */
    @Override
    public SysUserDO getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 按用户名查用户。
     */
    @Override
    public SysUserDO getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 新增一条用户记录。
     */
    @Override
    public void save(SysUserDO user) {
        userMapper.insert(user);
    }

    /**
     * 按 id 更新用户。
     */
    @Override
    public void updateById(SysUserDO user) {
        userMapper.updateById(user);
    }

    /**
     * 按 id 删掉用户。
     */
    @Override
    public void removeById(Long id) {
        userMapper.deleteById(id);
    }

    /**
     * 查当前用户总数。
     */
    @Override
    public long count() {
        return userMapper.selectCount(null);
    }
}
