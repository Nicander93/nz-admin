package com.nz.admin.modules.system.service.impl;

import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单这块的服务实现。
 */
@Service
public class SysMenuServiceImpl implements SysMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    /**
     * 把所有菜单按排序字段查出来。
     */
    @Override
    public List<SysMenu> listAll() {
        return menuMapper.selectListOrderBySort();
    }

    /**
     * 按 id 拿菜单详情。
     */
    @Override
    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    /**
     * 新增一条菜单记录。
     */
    @Override
    public void save(SysMenu menu) {
        menuMapper.insert(menu);
    }

    /**
     * 按 id 更新菜单。
     */
    @Override
    public void updateById(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    /**
     * 按 id 删掉菜单。
     */
    @Override
    public void removeById(Long id) {
        menuMapper.deleteById(id);
    }
}
