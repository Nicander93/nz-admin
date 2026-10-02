package com.nz.admin;

import com.nz.admin.framework.test.core.http.ApiTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("e2e")
class NzAdminE2eProfileTest {

    private final TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    NzAdminE2eProfileTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void authenticatesAndServesCoreAdminPagesWithH2() {
        var api = new ApiTestClient(restTemplate)
                .login("default", "admin", "admin123");
        for (String path : List.of("/api/auth/info", "/api/auth/menus",
                "/api/system/config/page?pageNum=1&pageSize=10", "/api/system/post/list", "/api/system/workbench/snapshot")) {
            api.ok(HttpMethod.GET, path, null);
        }
        jdbc.update("UPDATE sys_role SET data_scope=2 WHERE id=1");
        try {
            assertThat(api.ok(HttpMethod.GET, "/api/system/user/page", null).path("total").asLong()).isZero();
            assertThat(api.ok(HttpMethod.GET, "/api/system/profile", null).path("id").asLong()).isEqualTo(1);
            api.ok(HttpMethod.PUT, "/api/system/profile",
                    Map.of("nickname", "个人修改", "gender", "0", "email", "", "phone", ""));
            assertThat(jdbc.queryForObject("SELECT nickname FROM sys_user WHERE id=1", String.class))
                    .isEqualTo("个人修改");
        } finally {
            jdbc.update("UPDATE sys_role SET data_scope=1 WHERE id=1");
        }
    }

}
