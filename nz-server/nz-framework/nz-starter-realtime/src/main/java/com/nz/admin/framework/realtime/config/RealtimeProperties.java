package com.nz.admin.framework.realtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 实时通信配置。
 */
@ConfigurationProperties(prefix = "nz.realtime")
public class RealtimeProperties {

    private boolean enabled = true;
    private Duration ticketTtl = Duration.ofSeconds(30);
    private Duration sseTimeout = Duration.ofMinutes(30);
    private List<String> allowedOrigins = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTicketTtl() {
        return ticketTtl;
    }

    public void setTicketTtl(Duration ticketTtl) {
        this.ticketTtl = ticketTtl;
    }

    public Duration getSseTimeout() {
        return sseTimeout;
    }

    public void setSseTimeout(Duration sseTimeout) {
        this.sseTimeout = sseTimeout;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
