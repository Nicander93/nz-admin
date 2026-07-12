package com.nz.admin.modules.system.config;

import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.service.menu.MenuService;
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

    @Test
    void ensureMenusRegistersClientPageAndCrudPermissions() {
        when(menuService.listAll()).thenReturn(List.of());
        ReflectionTestUtils.invokeMethod(dataInitializer, "ensureMenus");

        ArgumentCaptor<MenuDO> menus = ArgumentCaptor.forClass(MenuDO.class);
        verify(menuService, atLeastOnce()).save(menus.capture());

        assertThat(menus.getAllValues()).extracting(MenuDO::getPerm)
                .contains("system:client:list", "system:client:query", "system:client:add",
                        "system:client:edit", "system:client:remove");
        assertThat(menus.getAllValues()).anySatisfy(menu -> {
            assertThat(menu.getPath()).isEqualTo("client");
            assertThat(menu.getComponent()).isEqualTo("system/client/index");
        });
    }
}