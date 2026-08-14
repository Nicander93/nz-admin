package com.nz.admin.framework.sms.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSmsGatewayTest {
    @Test
    void delegatesToProviderIgnoringCodeCase() {
        SmsProvider provider = new SmsProvider() {
            public String code() {
                return "demo";
            }
            public SmsSendResult send(SmsChannelConfig channel, SmsMessage message) {
                return SmsSendResult.accepted(message.phoneNumber());
            }
        };
        SmsGateway gateway = new DefaultSmsGateway(List.of(provider));
        SmsSendResult result = gateway.send(
                new SmsChannelConfig("main", "DEMO", null, null, null, null),
                new SmsMessage("13800138000", "notice", "hello", Map.of()));
        assertThat(result.providerMessageId()).isEqualTo("13800138000");
    }

    @Test
    void rejectsUnknownProvider() {
        SmsGateway gateway = new DefaultSmsGateway(List.of());
        assertThatThrownBy(() -> gateway.send(
                new SmsChannelConfig("main", "missing", null, null, null, null),
                new SmsMessage("13800138000", "notice", "hello", Map.of())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing");
    }
}
