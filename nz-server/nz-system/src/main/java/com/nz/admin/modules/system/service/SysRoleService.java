package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.query.SysRoleQuery;

import java.util.List;

public interface SysRoleService {

    Page<SysRole> listPage(SysRoleQuery query);

    List<SysRole> listAll();

    SysRole getById(Long id);

    void save(SysRole role);

    void updateById(SysRole role);

    void removeById(Long id);

    List<Long> getMenuIdsByRoleId(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
