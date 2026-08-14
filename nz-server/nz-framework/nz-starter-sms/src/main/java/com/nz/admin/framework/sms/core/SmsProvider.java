package com.nz.admin.framework.sms.core;

/** 短信供应商扩展点。 */
public interface SmsProvider {
    String code();
    SmsSendResult send(SmsChannelConfig channel, SmsMessage message);
}
