package com.nz.admin.framework.test.core.http;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

/** 真实 HTTP 测试客户端；不绑定应用启动类、端口或测试数据库。 */
public class ApiTestClient {
    private final TestRestTemplate rest;
    private final String token;

    public ApiTestClient(TestRestTemplate rest) {
        this(rest, null);
    }

    private ApiTestClient(TestRestTemplate rest, String token) {
        this.rest = rest;
        this.token = token;
    }

    public ApiTestClient login(String tenantCode, String username, String password) {
        JsonNode response =
                ok(
                        HttpMethod.POST,
                        "/api/auth/login",
                        Map.of(
                                "tenantCode",
                                tenantCode,
                                "clientId",
                                "nz-web-account",
                                "username",
                                username,
                                "password",
                                password));
        String value = response.asText();
        if (value.isBlank()) {
            throw new AssertionError("登录未返回令牌");
        }
        return new ApiTestClient(rest, value);
    }

    public JsonNode exchange(HttpMethod method, String path, Object body) {
        return exchange(method, path, body, Map.of());
    }

    /** 为并发、幂等和客户端协议测试提供显式请求头。 */
    public JsonNode exchange(HttpMethod method, String path, Object body, Map<String, String> extraHeaders) {
        HttpHeaders headers = new HttpHeaders();
        extraHeaders.forEach(headers::set);
        if (token != null) {
            headers.set("Authorization", token);
        }
        ResponseEntity<JsonNode> response =
                rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AssertionError(method + " " + path + " HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    public JsonNode ok(HttpMethod method, String path, Object body) {
        JsonNode response = exchange(method, path, body);
        if (response.path("code").asInt() != 200) {
            throw new AssertionError(method + " " + path + ": " + response.path("msg").asText());
        }
        return response.path("data");
    }
}
