package com.nz.admin.framework.sms.core;

import java.util.Map;

/** 渠道无关的短信消息。 */
public record SmsMessage(String phoneNumber, String templateCode, String content,
                         Map<String, Object> parameters) {
    public SmsMessage {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
