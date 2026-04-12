package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.mapper.SysUserMapper;
import com.nz.admin.modules.system.query.SysUserQuery;
import com.nz.admin.modules.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public Page<SysUser> listPage(SysUserQuery query) {
        return userMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public void save(SysUser user) {
        userMapper.insert(user);
    }

    @Override
    public void updateById(SysUser user) {
        userMapper.updateById(user);
    }

    @Override
    public void removeById(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public long count() {
        return userMapper.selectCount(null);
    }
}
