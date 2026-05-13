package com.nz.admin.modules.system.service.menu;

import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;

import java.util.List;

public interface MenuService {

    List<MenuDO> listAll();

    MenuDO getById(Long id);

    void save(MenuDO menu);

    void updateById(MenuDO menu);

    void removeById(Long id);
}
