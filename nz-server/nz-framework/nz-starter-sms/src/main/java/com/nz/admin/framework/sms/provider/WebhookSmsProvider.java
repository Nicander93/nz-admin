package com.nz.admin.framework.sms.provider;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.sms.core.SmsChannelConfig;
import com.nz.admin.framework.sms.core.SmsMessage;
import com.nz.admin.framework.sms.core.SmsProvider;
import com.nz.admin.framework.sms.core.SmsSendResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 通用 Webhook 短信渠道。 */
public class WebhookSmsProvider implements SmsProvider {
    private final RestClient restClient;

    public WebhookSmsProvider(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String code() {
        return "webhook";
    }

    @Override
    public SmsSendResult send(SmsChannelConfig channel, SmsMessage message) {
        if (StrUtil.isBlank(channel.endpoint())) {
            throw new IllegalArgumentException("Webhook 短信渠道必须配置 endpoint");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channelCode", channel.channelCode());
        body.put("phoneNumber", message.phoneNumber());
        body.put("templateCode", message.templateCode());
        body.put("content", message.content());
        body.put("parameters", message.parameters());
        body.put("signature", channel.signature());
        ResponseEntity<Void> response = restClient.post()
                .uri(channel.endpoint())
                .headers(headers -> applyCredentials(headers, channel))
                .body(body)
                .retrieve()
                .toBodilessEntity();
        String messageId = response.getHeaders().getFirst("X-Message-Id");
        return SmsSendResult.accepted(StrUtil.blankToDefault(messageId, "webhook-" + UUID.randomUUID()));
    }

    private void applyCredentials(HttpHeaders headers, SmsChannelConfig channel) {
        if (StrUtil.isNotBlank(channel.accessKeyId())) {
            headers.set("X-Access-Key", channel.accessKeyId());
        }
        if (StrUtil.isNotBlank(channel.accessKeySecret())) {
            headers.setBearerAuth(channel.accessKeySecret());
        }
    }
}
