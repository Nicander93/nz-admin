package com.nz.admin.framework.auth.aspect;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = SaCheckPermissionAspectTest.Application.class
)
@Import(SaCheckPermissionAspectTest.TestConfiguration.class)
class SaCheckPermissionAspectTest {

    private static final ThreadLocal<Set<String>> PERMISSION_HOLDER = new ThreadLocal<>();

    @Resource
    private PermissionTargetService permissionTargetService;

    @AfterEach
    void tearDown() {
        PERMISSION_HOLDER.remove();
        StpUtil.logout();
    }

    @Test
    void testHasPermission_shouldPass() {
        PERMISSION_HOLDER.set(Set.of("system:test:view"));
        StpUtil.login(1001L);

        String result = permissionTargetService.readProtectedResource();

        assertEquals("ok", result);
    }

    @Test
    void testNoPermission_shouldThrow() {
        PERMISSION_HOLDER.set(Set.of("system:test:edit"));
        StpUtil.login(1002L);

        assertThrows(NotPermissionException.class, permissionTargetService::readProtectedResource);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class
    })
    static class Application {
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        SaCheckPermissionAspect saCheckPermissionAspect() {
            return new SaCheckPermissionAspect();
        }

        @Bean
        PermissionTargetService permissionTargetService() {
            return new PermissionTargetService();
        }

        @Bean
        StpInterface stpInterface() {
            return new StpInterface() {
                @Override
                public List<String> getPermissionList(Object loginId, String loginType) {
                    Set<String> permissions = PERMISSION_HOLDER.get();
                    return permissions == null ? List.of() : new ArrayList<>(permissions);
                }

                @Override
                public List<String> getRoleList(Object loginId, String loginType) {
                    return List.of();
                }
            };
        }
    }

    @Service
    static class PermissionTargetService {

        @SaCheckPermission("system:test:view")
        public String readProtectedResource() {
            return "ok";
        }
    }
}
