package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.query.SysUserQuery;

public interface SysUserService {

    Page<SysUser> listPage(SysUserQuery query);

    SysUser getById(Long id);

    SysUser getByUsername(String username);

    void save(SysUser user);

    void updateById(SysUser user);

    void removeById(Long id);

    long count();
}
