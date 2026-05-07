package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysRoleDO;
import com.nz.admin.modules.system.entity.query.SysRoleQuery;

import java.util.List;

public interface SysRoleService {

    Page<SysRoleDO> listPage(SysRoleQuery query);

    List<SysRoleDO> listAll();

    SysRoleDO getById(Long id);

    void save(SysRoleDO role);

    void updateById(SysRoleDO role);

    void removeById(Long id);

    List<Long> getMenuIdsByRoleId(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
