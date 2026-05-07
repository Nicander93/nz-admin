package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.po.SysMenuDO;

import java.util.List;

public interface SysMenuService {

    List<SysMenuDO> listAll();

    SysMenuDO getById(Long id);

    void save(SysMenuDO menu);

    void updateById(SysMenuDO menu);

    void removeById(Long id);
}
