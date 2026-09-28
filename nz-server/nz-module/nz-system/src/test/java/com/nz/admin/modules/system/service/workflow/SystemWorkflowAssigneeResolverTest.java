package com.nz.admin.modules.system.service.workflow;

import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SystemWorkflowAssigneeResolverTest extends BaseMockitoUnitTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;

    private SystemWorkflowAssigneeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SystemWorkflowAssigneeResolver(userMapper, roleMapper, userRoleMapper);
    }

    @Test
    void resolvesUsersAndRolesAndFiltersDisabledUsers() {
        when(roleMapper.selectActiveByKeys(java.util.Set.of("manager"))).thenReturn(List.of(
                new RoleDO().setId(2L).setRoleKey("manager").setStatus(0)
        ));
        when(userRoleMapper.selectByRoleIds(List.of(2L))).thenReturn(List.of(
                new UserRoleDO().setUserId(8L).setRoleId(2L),
                new UserRoleDO().setUserId(9L).setRoleId(2L)
        ));
        when(userMapper.selectActiveByIds(java.util.Set.of(7L, 8L, 9L))).thenReturn(List.of(
                new UserDO().setId(7L).setStatus(0),
                new UserDO().setId(8L).setStatus(0)
        ));

        List<String> result = resolver.resolveUserIds(
                List.of("user:7", "role:manager", "7", "invalid", "user:-1"));

        assertThat(result).containsExactly("7", "8");
    }

    @Test
    void returnsEmptyForEmptyExpressions() {
        assertThat(resolver.resolveUserIds(List.of())).isEmpty();
    }
}
