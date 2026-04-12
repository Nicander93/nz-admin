package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.entity.SysRoleMenu;
import com.nz.admin.modules.system.mapper.SysRoleMapper;
import com.nz.admin.modules.system.mapper.SysRoleMenuMapper;
import com.nz.admin.modules.system.query.SysRoleQuery;
import com.nz.admin.modules.system.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    public Page<SysRole> listPage(SysRoleQuery query) {
        return roleMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public List<SysRole> listAll() {
        return roleMapper.selectList(null);
    }

    @Override
    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public void save(SysRole role) {
        roleMapper.insert(role);
    }

    @Override
    public void updateById(SysRole role) {
        roleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void removeById(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId).toList();
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }
}
