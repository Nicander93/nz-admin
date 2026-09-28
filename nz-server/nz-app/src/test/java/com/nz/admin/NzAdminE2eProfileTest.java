package com.nz.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

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
    NzAdminE2eProfileTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void authenticatesAndServesCoreAdminPagesWithH2() {
        ResponseEntity<JsonNode> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of(
                        "tenantCode", "default",
                        "clientId", "nz-web-account",
                        "username", "admin",
                        "password", "admin123"
                ),
                JsonNode.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().path("data").asText();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        assertOk("/api/auth/info", request);
        assertOk("/api/auth/menus", request);
        assertOk("/api/system/config/page?pageNum=1&pageSize=10", request);
        assertOk("/api/system/post/list", request);
        assertOk("/api/system/workbench/snapshot", request);
    }

    private void assertOk(String path, HttpEntity<Void> request) {
        ResponseEntity<JsonNode> response =
                restTemplate.exchange(path, HttpMethod.GET, request, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).isEqualTo(200);
    }
}
