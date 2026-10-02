package com.nz.admin.modules.system.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.entity.dataobject.role.*;
import com.nz.admin.modules.system.entity.dataobject.user.*;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.mapper.role.*;
import com.nz.admin.modules.system.mapper.user.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

class SystemDataScopeResolverTest extends BaseMockitoUnitTest {
    @Mock LoginUserContext loginUserContext;
    @Mock UserMapper userMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock RoleMapper roleMapper;
    @Mock RoleDeptMapper roleDeptMapper;
    @Mock DeptMapper deptMapper;
    @InjectMocks SystemDataScopeResolver resolver;

    @BeforeEach
    void identity() {
        when(loginUserContext.getLoginUserIdOrNull()).thenReturn(100L);
        when(userMapper.selectById(100L))
                .thenReturn(new UserDO().setId(100L).setDeptId(10L).setStatus(0));
    }

    private RoleDO role(long id, int range, int status) {
        var relation = new UserRoleDO();
        relation.setUserId(100L);
        relation.setRoleId(id);
        var role = new RoleDO().setId(id).setDataScope(range).setStatus(status);
        when(roleMapper.selectById(id)).thenReturn(role);
        return role;
    }

    @Test
    void combinesCustomDepartmentsDescendantsAndSelfButIgnoresDisabledRoles() {
        role(1, 2, 0);
        role(2, 4, 0);
        role(3, 5, 0);
        role(4, 1, 1);
        when(userRoleMapper.selectByUserId(100L))
                .thenReturn(
                        java.util.stream.LongStream.rangeClosed(1, 4)
                                .mapToObj(id -> new UserRoleDO().setUserId(100L).setRoleId(id))
                                .toList());
        var custom = new RoleDeptDO();
        custom.setDeptId(20L);
        when(roleDeptMapper.selectByRoleId(1L)).thenReturn(List.of(custom));
        when(deptMapper.selectList(null))
                .thenReturn(
                        List.of(
                                new DeptDO().setId(11L).setParentId(10L),
                                new DeptDO().setId(12L).setParentId(11L)));
        var result = resolver.resolve();
        assertThat(result.all()).isFalse();
        assertThat(result.self()).isTrue();
        assertThat(result.deptIds()).containsExactlyInAnyOrder(10L, 11L, 12L, 20L);
    }

    @Test
    void departmentOnlyAndAllDataAreResolvedFromRoleConfiguration() {
        role(1, 3, 0);
        when(userRoleMapper.selectByUserId(100L))
                .thenReturn(List.of(new UserRoleDO().setUserId(100L).setRoleId(1L)));
        assertThat(resolver.resolve().deptIds()).containsExactly(10L);
        role(1, 1, 0);
        assertThat(resolver.resolve().all()).isTrue();
    }

    @Test
    void noRolesDeniesAllRowsAndInvalidRangeFails() {
        when(userRoleMapper.selectByUserId(100L)).thenReturn(List.of());
        var result = resolver.resolve();
        assertThat(result.all()).isFalse();
        assertThat(result.self()).isFalse();
        assertThat(result.deptIds()).isEmpty();
        role(1, 99, 0);
        when(userRoleMapper.selectByUserId(100L))
                .thenReturn(List.of(new UserRoleDO().setUserId(100L).setRoleId(1L)));
        assertThatThrownBy(resolver::resolve).hasMessageContaining("范围不合法");
    }
}
