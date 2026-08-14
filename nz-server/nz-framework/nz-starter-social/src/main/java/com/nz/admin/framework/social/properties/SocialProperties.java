package com.nz.admin.framework.social.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OAuth2/OIDC 社交认证配置。 */
@Data
@ConfigurationProperties(prefix = "nz.social")
public class SocialProperties {
    private boolean enabled = true;
    private Duration stateTtl = Duration.ofMinutes(5);
    private Map<String, Provider> providers = new LinkedHashMap<>();

    /** 单个 OAuth2 或 OIDC 服务商配置。 */
    @Data
    public static class Provider {
        private boolean enabled;
        private String displayName;
        private String protocol = "oauth2";
        private String clientId;
        private String clientSecret;
        private String clientAuthenticationMethod = "client_secret_post";
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String issuerUri;
        private String jwkSetUri;
        private String redirectUri;
        private List<String> scopes = new ArrayList<>();
        private String subjectAttribute = "sub";
        private String usernameAttribute = "preferred_username";
        private String nicknameAttribute = "name";
        private String emailAttribute = "email";
        private String avatarAttribute = "picture";
        private Map<String, String> authorizationParameters = new LinkedHashMap<>();
    }
}
