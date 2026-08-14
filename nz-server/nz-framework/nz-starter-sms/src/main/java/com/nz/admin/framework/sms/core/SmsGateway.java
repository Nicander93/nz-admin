package com.nz.admin.framework.sms.core;

/** 业务模块使用的短信发送端口。 */
public interface SmsGateway {
    SmsSendResult send(SmsChannelConfig channel, SmsMessage message);
}
