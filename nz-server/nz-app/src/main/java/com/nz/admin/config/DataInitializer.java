package com.nz.admin.config;

import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.modules.system.entity.*;
import com.nz.admin.modules.system.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysRoleService roleService;
    @Autowired
    private SysMenuService menuService;
    @Autowired
    private SysDeptService deptService;
    @Autowired
    private SysPermissionService permissionService;

    @Override
    public void run(String... args) {
        if (userService.count() > 0) return;

        log.info("初始化基础数据...");

        // 部门
        SysDept rootDept = new SysDept();
        rootDept.setParentId(0L);
        rootDept.setName("总公司");
        rootDept.setSort(0);
        rootDept.setStatus(0);
        deptService.save(rootDept);

        // 管理员用户
        SysUser admin = new SysUser();
        admin.setDeptId(rootDept.getId());
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123"));
        admin.setNickname("管理员");
        admin.setStatus(0);
        userService.save(admin);

        // 超级管理员角色
        SysRole adminRole = new SysRole();
        adminRole.setName("超级管理员");
        adminRole.setRoleKey("admin");
        adminRole.setSort(0);
        adminRole.setStatus(0);
        roleService.save(adminRole);

        // 菜单 - 系统管理
        SysMenu systemDir = createMenu(0L, "系统管理", "/system", null, "Setting", 0, "M", null, 0);
        menuService.save(systemDir);

        // 用户管理
        SysMenu userMenu = createMenu(systemDir.getId(), "用户管理", "user", "system/user/index", "User", 1, "C", "system:user:list", 0);
        menuService.save(userMenu);
        saveButtons(userMenu.getId(), "system:user", 2);

        // 角色管理
        SysMenu roleMenu = createMenu(systemDir.getId(), "角色管理", "role", "system/role/index", "UserFilled", 2, "C", "system:role:list", 0);
        menuService.save(roleMenu);
        saveButtons(roleMenu.getId(), "system:role", 3);

        // 部门管理
        SysMenu deptMenu = createMenu(systemDir.getId(), "部门管理", "dept", "system/dept/index", "OfficeBuilding", 3, "C", "system:dept:list", 0);
        menuService.save(deptMenu);
        saveButtons(deptMenu.getId(), "system:dept", 4);

        // 菜单管理
        SysMenu menuMenu = createMenu(systemDir.getId(), "菜单管理", "menu", "system/menu/index", "Menu", 4, "C", "system:menu:list", 0);
        menuService.save(menuMenu);
        saveButtons(menuMenu.getId(), "system:menu", 5);

        // 字典管理
        SysMenu dictMenu = createMenu(systemDir.getId(), "字典管理", "dict", "system/dict/index", "Collection", 5, "C", "system:dict:list", 0);
        menuService.save(dictMenu);
        saveButtons(dictMenu.getId(), "system:dict", 6);

        // 分配管理员所有菜单权限
        var allMenus = menuService.listAll();
        roleService.assignMenus(adminRole.getId(), allMenus.stream().map(SysMenu::getId).toList());

        // 分配管理员角色
        permissionService.assignUserRoles(admin.getId(), java.util.List.of(adminRole.getId()));

        log.info("初始化完成: admin / admin123");
    }

    private SysMenu createMenu(Long parentId, String name, String path, String component, String icon, int sort, String type, String perm, int visible) {
        SysMenu menu = new SysMenu();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setSort(sort);
        menu.setType(type);
        menu.setPerm(perm);
        menu.setVisible(visible);
        menu.setStatus(0);
        return menu;
    }

    private void saveButtons(Long parentId, String permPrefix, int sortBase) {
        String[][] buttons = {
            {"查询", permPrefix + ":query"},
            {"新增", permPrefix + ":add"},
            {"修改", permPrefix + ":edit"},
            {"删除", permPrefix + ":remove"},
        };
        for (int i = 0; i < buttons.length; i++) {
            SysMenu btn = createMenu(parentId, buttons[i][0], null, null, null, sortBase * 100 + i, "F", buttons[i][1], 0);
            menuService.save(btn);
        }
    }
}
