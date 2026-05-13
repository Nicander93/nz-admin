package com.nz.admin.modules.system.service.role;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.query.role.RoleQuery;

import java.util.List;

public interface RoleService {

    Page<RoleDO> listPage(RoleQuery query);

    List<RoleDO> listAll();

    RoleDO getById(Long id);

    void save(RoleDO role);

    void updateById(RoleDO role);

    void removeById(Long id);

    List<Long> getMenuIdsByRoleId(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
