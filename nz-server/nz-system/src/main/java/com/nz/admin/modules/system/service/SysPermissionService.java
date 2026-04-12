package com.nz.admin.modules.system.service;

import java.util.List;
import java.util.Set;

public interface SysPermissionService {

    Set<String> getRoleKeysByUserId(Long userId);

    Set<String> getPermsByUserId(Long userId);

    List<Long> getRoleIdsByUserId(Long userId);

    void assignUserRoles(Long userId, List<Long> roleIds);
}
