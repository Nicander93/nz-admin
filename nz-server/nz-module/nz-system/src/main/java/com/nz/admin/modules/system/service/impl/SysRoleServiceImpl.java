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

/**
 * 角色这块的服务实现。
 */
@Service
public class SysRoleServiceImpl implements SysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    /**
     * 按分页条件查角色列表。
     */
    @Override
    public Page<SysRole> listPage(SysRoleQuery query) {
        return roleMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 把所有角色查出来。
     */
    @Override
    public List<SysRole> listAll() {
        return roleMapper.selectList(null);
    }

    /**
     * 按 id 拿角色详情。
     */
    @Override
    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 新增一条角色记录。
     */
    @Override
    public void save(SysRole role) {
        roleMapper.insert(role);
    }

    /**
     * 按 id 更新角色。
     */
    @Override
    public void updateById(SysRole role) {
        roleMapper.updateById(role);
    }

    /**
     * 删除角色，并把角色菜单关联一并清掉。
     */
    @Override
    @Transactional
    public void removeById(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    /**
     * 按角色 id 拿菜单 id 列表。
     */
    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 给角色重新分配菜单。
     */
    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 这里走覆盖式分配：先清掉旧关联，再写入新关联。
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }
}
