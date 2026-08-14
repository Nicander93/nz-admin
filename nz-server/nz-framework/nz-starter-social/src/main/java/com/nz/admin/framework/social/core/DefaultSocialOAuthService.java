package com.nz.admin.framework.social.core;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.social.properties.SocialProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 标准 OAuth2/OIDC 实现。 */
public class DefaultSocialOAuthService implements SocialOAuthService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final SocialProperties properties;
    private final SocialAuthorizationStateStore stateStore;
    private final RestClient restClient;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();

    public DefaultSocialOAuthService(SocialProperties properties,
                                     SocialAuthorizationStateStore stateStore,
                                     RestClient restClient,
                                     Clock clock,
                                     SecureRandom secureRandom) {
        this.properties = properties;
        this.stateStore = stateStore;
        this.restClient = restClient;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Override
    public List<SocialProvider> providers() {
        return properties.getProviders().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(entry -> new SocialProvider(entry.getKey(),
                        StrUtil.blankToDefault(entry.getValue().getDisplayName(), entry.getKey())))
                .toList();
    }

    @Override
    public SocialAuthorization authorize(String providerCode, SocialAuthorizationContext context) {
        SocialProperties.Provider provider = requireProvider(providerCode);
        validateProvider(provider);
        String state = randomUrlSafe(32);
        String codeVerifier = randomUrlSafe(64);
        String codeChallenge = sha256UrlSafe(codeVerifier);
        Instant expiresAt = clock.instant().plus(properties.getStateTtl());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", provider.getClientId())
                .queryParam("redirect_uri", provider.getRedirectUri())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256");
        if (!provider.getScopes().isEmpty()) {
            builder.queryParam("scope", String.join(" ", provider.getScopes()));
        }
        provider.getAuthorizationParameters().forEach(builder::queryParam);

        stateStore.save(state, new PendingSocialAuthorization(
                providerCode, context, codeVerifier, provider.getRedirectUri(), expiresAt));
        return new SocialAuthorization(builder.build().encode().toUriString(), state, expiresAt);
    }

    @Override
    public SocialCallbackResult callback(String providerCode, String code, String state) {
        if (StrUtil.isBlank(code) || StrUtil.isBlank(state)) {
            throw new SocialAuthenticationException("第三方授权回调缺少 code 或 state");
        }
        PendingSocialAuthorization pending = stateStore.consume(state);
        if (!pending.provider().equals(providerCode)) {
            throw new SocialAuthenticationException("第三方授权服务商与 state 不匹配");
        }

        SocialProperties.Provider provider = requireProvider(providerCode);
        Map<String, Object> token = exchangeToken(provider, code, pending);
        Map<String, Object> claims = loadVerifiedClaims(providerCode, provider, token);
        String subject = attribute(claims, provider.getSubjectAttribute());
        if (StrUtil.isBlank(subject)) {
            throw new SocialAuthenticationException("第三方身份缺少稳定用户标识");
        }
        SocialIdentity identity = new SocialIdentity(
                providerCode, subject,
                attribute(claims, provider.getUsernameAttribute()),
                attribute(claims, provider.getNicknameAttribute()),
                attribute(claims, provider.getEmailAttribute()),
                attribute(claims, provider.getAvatarAttribute())
        );
        return new SocialCallbackResult(pending.context(), identity);
    }

    private Map<String, Object> exchangeToken(SocialProperties.Provider provider, String code,
                                              PendingSocialAuthorization pending) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", pending.redirectUri());
        form.add("code_verifier", pending.codeVerifier());

        HttpHeaders headers = new HttpHeaders();
        String method = StrUtil.blankToDefault(
                provider.getClientAuthenticationMethod(), "client_secret_post");
        if ("client_secret_basic".equals(method)) {
            headers.setBasicAuth(provider.getClientId(), provider.getClientSecret());
        } else {
            form.add("client_id", provider.getClientId());
            if (!"none".equals(method)) {
                form.add("client_secret", provider.getClientSecret());
            }
        }
        try {
            Map<String, Object> body = restClient.post()
                    .uri(provider.getTokenUri())
                    .headers(value -> value.addAll(headers))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP_TYPE);
            if (body == null) {
                throw new SocialAuthenticationException("第三方令牌接口返回空响应");
            }
            return body;
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new SocialAuthenticationException("第三方令牌交换失败", e);
        }
    }

    private Map<String, Object> loadVerifiedClaims(String providerCode,
                                                   SocialProperties.Provider provider,
                                                   Map<String, Object> token) {
        Map<String, Object> claims = new LinkedHashMap<>();
        String idToken = stringValue(token.get("id_token"));
        if ("oidc".equalsIgnoreCase(provider.getProtocol())) {
            if (StrUtil.isBlank(idToken)) {
                throw new SocialAuthenticationException("OIDC 令牌响应缺少 id_token");
            }
            try {
                claims.putAll(jwtDecoders.computeIfAbsent(providerCode,
                        ignored -> createJwtDecoder(provider)).decode(idToken).getClaims());
            } catch (JwtException e) {
                throw new SocialAuthenticationException("OIDC ID Token 校验失败", e);
            }
        }

        if (StrUtil.isNotBlank(provider.getUserInfoUri())) {
            String accessToken = stringValue(token.get("access_token"));
            if (StrUtil.isBlank(accessToken)) {
                throw new SocialAuthenticationException("第三方令牌响应缺少 access_token");
            }
            try {
                Map<String, Object> userInfo = restClient.get()
                        .uri(provider.getUserInfoUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(MAP_TYPE);
                if (userInfo != null) {
                    claims.putAll(userInfo);
                }
            } catch (RestClientResponseException | ResourceAccessException e) {
                throw new SocialAuthenticationException("获取第三方用户信息失败", e);
            }
        }
        if (claims.isEmpty()) {
            throw new SocialAuthenticationException("未配置可验证的 OIDC 或用户信息接口");
        }
        return claims;
    }

    private JwtDecoder createJwtDecoder(SocialProperties.Provider provider) {
        NimbusJwtDecoder decoder;
        if (StrUtil.isNotBlank(provider.getJwkSetUri())) {
            decoder = NimbusJwtDecoder.withJwkSetUri(provider.getJwkSetUri()).build();
        } else if (StrUtil.isNotBlank(provider.getIssuerUri())) {
            JwtDecoder discovered = JwtDecoders.fromIssuerLocation(provider.getIssuerUri());
            if (!(discovered instanceof NimbusJwtDecoder nimbus)) {
                return discovered;
            }
            decoder = nimbus;
        } else {
            throw new SocialAuthenticationException(
                    "OIDC 服务商必须配置 issuer-uri 或 jwk-set-uri");
        }

        OAuth2TokenValidator<Jwt> base = StrUtil.isNotBlank(provider.getIssuerUri())
                ? JwtValidators.createDefaultWithIssuer(provider.getIssuerUri())
                : JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(provider.getClientId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "ID Token audience 不匹配", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(base, audience));
        return decoder;
    }

    private SocialProperties.Provider requireProvider(String providerCode) {
        SocialProperties.Provider provider = properties.getProviders().get(providerCode);
        if (provider == null || !provider.isEnabled()) {
            throw new SocialAuthenticationException("第三方登录服务商未启用");
        }
        return provider;
    }

    private void validateProvider(SocialProperties.Provider provider) {
        if (StrUtil.hasBlank(provider.getClientId(), provider.getAuthorizationUri(),
                provider.getTokenUri(), provider.getRedirectUri())) {
            throw new SocialAuthenticationException("第三方登录服务商配置不完整");
        }
        if (!"none".equals(provider.getClientAuthenticationMethod())
                && StrUtil.isBlank(provider.getClientSecret())) {
            throw new SocialAuthenticationException("第三方登录服务商缺少 client-secret");
        }
    }

    private String randomUrlSafe(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String sha256UrlSafe(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少 SHA-256", e);
        }
    }

    private String attribute(Map<String, Object> claims, String name) {
        return StrUtil.isBlank(name) ? null : stringValue(claims.get(name));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
