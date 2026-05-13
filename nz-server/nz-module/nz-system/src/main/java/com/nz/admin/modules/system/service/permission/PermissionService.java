package com.nz.admin.modules.system.service.permission;

import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getRoleKeysByUserId(Long userId);

    Set<String> getPermsByUserId(Long userId);

    List<MenuDO> getMenusByUserId(Long userId);

    List<Long> getRoleIdsByUserId(Long userId);

    void assignUserRoles(Long userId, List<Long> roleIds);
}
