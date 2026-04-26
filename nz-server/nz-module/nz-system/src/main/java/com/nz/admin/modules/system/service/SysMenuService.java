package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysMenu;

import java.util.List;

public interface SysMenuService {

    List<SysMenu> listAll();

    SysMenu getById(Long id);

    void save(SysMenu menu);

    void updateById(SysMenu menu);

    void removeById(Long id);
}
