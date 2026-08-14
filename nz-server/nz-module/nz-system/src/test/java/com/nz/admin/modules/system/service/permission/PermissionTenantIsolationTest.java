package com.nz.admin.modules.system.service.permission;

import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import com.nz.admin.modules.system.service.tenant.TenantPackageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 套餐缩减后的权限读取回归测试。
 */
class PermissionTenantIsolationTest extends BaseMockitoUnitTest {

    @InjectMocks
    private PermissionServiceImpl permissionService;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private TenantPackageService tenantPackageService;
    @Mock
    private TenantProperties tenantProperties;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldExcludeRoleMenusOutsideCurrentPackage() {
        TenantContextHolder.setTenantId(2L);
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);
        when(tenantMapper.selectById(2L)).thenReturn(new TenantDO().setId(2L).setPackageId(7L));
        when(tenantPackageService.getMenuIds(7L)).thenReturn(Set.of(10L));
        when(userRoleMapper.selectByUserId(9L))
                .thenReturn(List.of(new UserRoleDO().setUserId(9L).setRoleId(3L)));
        when(roleMenuMapper.selectByRoleId(3L)).thenReturn(List.of(
                new RoleMenuDO().setRoleId(3L).setMenuId(10L),
                new RoleMenuDO().setRoleId(3L).setMenuId(11L)
        ));
        when(menuMapper.selectById(10L)).thenReturn(new MenuDO().setId(10L).setPerm("system:user:list"));

        Set<String> permissions = permissionService.getPermsByUserId(9L);

        assertThat(permissions).containsExactly("system:user:list");
    }
}
