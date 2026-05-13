package com.nz.admin.modules.system.service.menu;

import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 菜单这块的服务实现。
 */
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;

    /**
     * 把所有菜单按排序字段查出来。
     */
    @Override
    public List<MenuDO> listAll() {
        return menuMapper.selectListOrderBySort();
    }

    /**
     * 按 id 拿菜单详情。
     */
    @Override
    public MenuDO getById(Long id) {
        return menuMapper.selectById(id);
    }

    /**
     * 新增一条菜单记录。
     */
    @Override
    public void save(MenuDO menu) {
        menuMapper.insert(menu);
    }

    /**
     * 按 id 更新菜单。
     */
    @Override
    public void updateById(MenuDO menu) {
        menuMapper.updateById(menu);
    }

    /**
     * 按 id 删掉菜单（含子菜单），并清理角色菜单关联。
     */
    @Override
    @Transactional
    public void removeById(Long id) {
        List<Long> allIds = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(id);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            allIds.add(cur);
            for (MenuDO child : menuMapper.selectByParentId(cur)) {
                stack.push(child.getId());
            }
        }
        roleMenuMapper.deleteByMenuIds(allIds);
        menuMapper.deleteBatchIds(allIds);
    }
}
