package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.entity.query.SysUserQuery;

public interface SysUserService {

    Page<SysUserDO> listPage(SysUserQuery query);

    SysUserDO getById(Long id);

    SysUserDO getByUsername(String username);

    void save(SysUserDO user);

    void updateById(SysUserDO user);

    void removeById(Long id);

    long count();
}
