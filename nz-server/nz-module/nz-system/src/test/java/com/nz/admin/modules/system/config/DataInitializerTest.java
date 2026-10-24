package com.nz.admin.modules.system.config;

import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.menu.MenuService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @InjectMocks
    private DataInitializer dataInitializer;
    @Mock
    private MenuService menuService;
    @Mock
    private UserService userService;

    @Test
    void ensureMenusRegistersClientPageAndCrudPermissions() {
        when(menuService.listAll()).thenReturn(List.of());
        ReflectionTestUtils.invokeMethod(dataInitializer, "ensureMenus");

        ArgumentCaptor<MenuDO> menus = ArgumentCaptor.forClass(MenuDO.class);
        verify(menuService, atLeastOnce()).save(menus.capture());

        assertThat(menus.getAllValues()).extracting(MenuDO::getPerm)
                .contains("system:client:list", "system:client:query", "system:client:add",
                        "system:client:edit", "system:client:remove", "system:fileconfig:test",
                        "system:online:list", "system:online:force",
                        "system:realtime:view", "system:realtime:send",
                        "system:sms:list", "system:sms:send");
        assertThat(menus.getAllValues()).anySatisfy(menu -> {
            assertThat(menu.getPath()).isEqualTo("client");
            assertThat(menu.getComponent()).isEqualTo("system/client/index");
        });
        assertThat(menus.getAllValues()).anySatisfy(menu -> {
            assertThat(menu.getPath()).isEqualTo("realtime");
            assertThat(menu.getComponent()).isEqualTo("system/realtime/index");
        });
        assertThat(menus.getAllValues()).anySatisfy(menu -> {
            assertThat(menu.getPath()).isEqualTo("sms");
            assertThat(menu.getComponent()).isEqualTo("system/sms/index");
        });
        assertThat(menus.getAllValues()).anySatisfy(menu -> {
            assertThat(menu.getPath()).isEqualTo("online");
            assertThat(menu.getComponent()).isEqualTo("system/online/index");
        });
    }


    @Test
    void ensureAdminUserUsesConfiguredInitialPassword() {
        ReflectionTestUtils.setField(dataInitializer, "initialAdminPassword", "Strong-Initial-Password");
        DeptDO rootDept = new DeptDO();
        rootDept.setId(10L);

        UserDO result = ReflectionTestUtils.invokeMethod(dataInitializer, "ensureAdminUser", rootDept);

        ArgumentCaptor<UserDO> user = ArgumentCaptor.forClass(UserDO.class);
        verify(userService).save(user.capture());
        assertThat(result).isSameAs(user.getValue());
        assertThat(user.getValue().getUsername()).isEqualTo("admin");
        assertThat(BCrypt.checkpw("Strong-Initial-Password", user.getValue().getPassword())).isTrue();
    }
}
