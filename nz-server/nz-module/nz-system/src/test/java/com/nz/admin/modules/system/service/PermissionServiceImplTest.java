package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import com.nz.admin.modules.system.service.permission.PermissionServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(NzSystemTestApplication.class)
class PermissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PermissionServiceImpl permissionService;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private com.nz.admin.modules.system.mapper.user.UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MenuMapper menuMapper;

    @Test
    void testGetRoleKeysByUserId() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null)
                .setRoleKey("admin").setStatus(0);
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);

        assertEquals(1, roleKeys.size());
        assertTrue(roleKeys.contains("admin"));
    }

    @Test
    void testGetRoleKeysByUserId_noRoles() {
        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);
        assertTrue(roleKeys.isEmpty());
    }

    @Test
    void testGetPermsByUserId() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null).setStatus(0);
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        MenuDO menu1 = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("system:user:list").setStatus(0).setType("C");
        menuMapper.insert(menu1);

        MenuDO menu2 = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("system:user:add").setStatus(0).setType("F");
        menuMapper.insert(menu2);

        RoleMenuDO roleMenu1 = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu1.getId());
        roleMenuMapper.insert(roleMenu1);

        RoleMenuDO roleMenu2 = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu2.getId());
        roleMenuMapper.insert(roleMenu2);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertEquals(2, perms.size());
        assertTrue(perms.contains("system:user:list"));
        assertTrue(perms.contains("system:user:add"));
    }

    @Test
    void testGetPermsByUserId_filterBlankPerms() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null).setStatus(0);
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        MenuDO menu = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("").setStatus(0).setType("F");
        menuMapper.insert(menu);

        RoleMenuDO roleMenu = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu.getId());
        roleMenuMapper.insert(roleMenu);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertTrue(perms.isEmpty());
    }

    @Test
    void disabledRoleImmediatelyLosesRolesMenusAndPermissions() {
        var role = createRoleAndMenu(0, 0, "F", 0);
        assertTrue(permissionService.getPermsByUserId(1L).contains("test:protected"));
        roleMapper.updateById(new RoleDO().setId(role.getId()).setStatus(1));

        assertTrue(permissionService.getRoleKeysByUserId(1L).isEmpty());
        assertTrue(permissionService.getPermsByUserId(1L).isEmpty());
        assertTrue(permissionService.getMenusByUserId(1L).isEmpty());
        // 管理页仍能看到已分配但禁用的角色，重新启用不需要重绑。
        assertEquals(List.of(role.getId()), permissionService.getRoleIdsByUserId(1L));
        roleMapper.updateById(new RoleDO().setId(role.getId()).setStatus(0));
        assertTrue(permissionService.getPermsByUserId(1L).contains("test:protected"));
    }

    @Test
    void disabledMenuIsExcludedFromBothPermissionsAndNavigation() {
        createRoleAndMenu(0, 1, "C", 0);
        assertTrue(permissionService.getPermsByUserId(1L).isEmpty());
        assertTrue(permissionService.getMenusByUserId(1L).isEmpty());
        assertEquals(Set.of("test-active-role"), permissionService.getRoleKeysByUserId(1L));
    }

    @Test
    void hiddenEnabledMenuStillGrantsItsPermission() {
        createRoleAndMenu(0, 0, "C", 1);
        assertEquals(Set.of("test:protected"), permissionService.getPermsByUserId(1L));
        assertEquals(1, permissionService.getMenusByUserId(1L).size());
    }

    @Test
    void directoryDoesNotGrantFunctionalPermission() {
        createRoleAndMenu(0, 0, "M", 0);
        assertTrue(permissionService.getPermsByUserId(1L).isEmpty());
        assertEquals(1, permissionService.getMenusByUserId(1L).size());
    }

    private RoleDO createRoleAndMenu(int roleStatus, int menuStatus, String menuType, int visible) {
        var role = new RoleDO().setName("测试角色").setRoleKey("test-active-role").setStatus(roleStatus).setSort(0);
        roleMapper.insert(role);
        var menu = new MenuDO().setName("测试菜单").setParentId(0L).setPerm("test:protected")
                .setStatus(menuStatus).setType(menuType).setVisible(visible).setSort(0);
        menuMapper.insert(menu);
        userRoleMapper.insert(new UserRoleDO().setUserId(1L).setRoleId(role.getId()));
        roleMenuMapper.insert(new RoleMenuDO().setRoleId(role.getId()).setMenuId(menu.getId()));
        return role;
    }

    @Test
    void testAssignUserRoles() {
        var user = new com.nz.admin.modules.system.entity.dataobject.user.UserDO().setId(1L).setUsername("assignment-target").setPassword("test").setStatus(0);
        userMapper.insert(user);
        UserRoleDO old = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(999L);
        userRoleMapper.insert(old);

        roleMapper.insert(randomPojo(RoleDO.class).setId(10L).setRoleKey("assign-role-10"));
        roleMapper.insert(randomPojo(RoleDO.class).setId(20L).setRoleKey("assign-role-20"));
        List<Long> roleIds = List.of(10L, 20L);
        permissionService.assignUserRoles(1L, roleIds);

        List<UserRoleDO> dbRows = userRoleMapper.selectByUserId(1L);
        assertEquals(2, dbRows.size());
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(10L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(20L)));
    }

    @Test
    void testGetRoleIdsByUserId() {
        UserRoleDO userRole1 = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(10L);
        userRoleMapper.insert(userRole1);

        UserRoleDO userRole2 = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(20L);
        userRoleMapper.insert(userRole2);

        List<Long> roleIds = permissionService.getRoleIdsByUserId(1L);

        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains(10L));
        assertTrue(roleIds.contains(20L));
    }
}
