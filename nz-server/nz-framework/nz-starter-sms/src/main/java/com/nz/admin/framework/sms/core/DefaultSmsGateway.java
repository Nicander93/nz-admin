package com.nz.admin.framework.sms.core;

import cn.hutool.core.util.StrUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 根据渠道配置选择供应商实现。 */
public class DefaultSmsGateway implements SmsGateway {
    private final Map<String, SmsProvider> providers;

    public DefaultSmsGateway(List<SmsProvider> providers) {
        Map<String, SmsProvider> resolved = new LinkedHashMap<>();
        for (SmsProvider provider : providers) {
            String code = normalize(provider.code());
            if (resolved.putIfAbsent(code, provider) != null) {
                throw new IllegalStateException("短信供应商编码重复: " + code);
            }
        }
        this.providers = Map.copyOf(resolved);
    }

    @Override
    public SmsSendResult send(SmsChannelConfig channel, SmsMessage message) {
        if (channel == null || StrUtil.isBlank(channel.providerCode())) {
            throw new IllegalArgumentException("短信渠道供应商不能为空");
        }
        SmsProvider provider = providers.get(normalize(channel.providerCode()));
        if (provider == null) {
            throw new IllegalArgumentException("不支持的短信供应商: " + channel.providerCode());
        }
        return provider.send(channel, message);
    }

    private String normalize(String code) {
        return StrUtil.trim(code).toLowerCase(Locale.ROOT);
    }
}
