package com.nz.admin.framework.sms.core;

/** 一次发送所需的渠道配置。 */
public record SmsChannelConfig(String channelCode, String providerCode, String endpoint,
                               String accessKeyId, String accessKeySecret, String signature) {
}
