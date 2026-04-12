package com.nz.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import com.nz.admin.modules.system.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysPermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return new ArrayList<>(permissionService.getPermsByUserId(userId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return new ArrayList<>(permissionService.getRoleKeysByUserId(userId));
    }
}
