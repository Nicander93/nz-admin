package com.nz.admin.framework.sms.core;

/** 短信渠道发送结果。 */
public record SmsSendResult(String providerMessageId) {
    public static SmsSendResult accepted(String providerMessageId) {
        return new SmsSendResult(providerMessageId);
    }
}
