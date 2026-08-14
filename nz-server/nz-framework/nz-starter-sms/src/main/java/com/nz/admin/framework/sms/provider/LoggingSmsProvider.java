package com.nz.admin.framework.sms.provider;

import com.nz.admin.framework.sms.core.SmsChannelConfig;
import com.nz.admin.framework.sms.core.SmsMessage;
import com.nz.admin.framework.sms.core.SmsProvider;
import com.nz.admin.framework.sms.core.SmsSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/** 本地开发使用的日志短信渠道。 */
public class LoggingSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsProvider.class);

    @Override
    public String code() {
        return "log";
    }

    @Override
    public SmsSendResult send(SmsChannelConfig channel, SmsMessage message) {
        String messageId = "log-" + UUID.randomUUID();
        log.info("SMS accepted: channel={}, phone={}, template={}, messageId={}",
                channel.channelCode(), message.phoneNumber(), message.templateCode(), messageId);
        return SmsSendResult.accepted(messageId);
    }
}
