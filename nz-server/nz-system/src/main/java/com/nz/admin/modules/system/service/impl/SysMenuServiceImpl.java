package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Override
    public List<SysMenu> listAll() {
        return menuMapper.selectListOrderBySort();
    }

    @Override
    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public void save(SysMenu menu) {
        menuMapper.insert(menu);
    }

    @Override
    public void updateById(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    @Override
    public void removeById(Long id) {
        menuMapper.deleteById(id);
    }
}
