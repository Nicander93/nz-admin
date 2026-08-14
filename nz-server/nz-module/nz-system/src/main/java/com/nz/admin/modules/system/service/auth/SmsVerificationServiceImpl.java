package com.nz.admin.modules.system.service.auth;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.config.SmsVerificationProperties;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.sms.SmsService;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/** 短信登录验证码服务实现。 */
@Service
public class SmsVerificationServiceImpl implements SmsVerificationService {
    private final SmsVerificationCodeStore codeStore;
    private final SmsVerificationProperties properties;
    private final Optional<SmsService> smsService;
    private final UserService userService;

    public SmsVerificationServiceImpl(SmsVerificationCodeStore codeStore,
                                      SmsVerificationProperties properties,
                                      Optional<SmsService> smsService,
                                      UserService userService) {
        this.codeStore = codeStore;
        this.properties = properties;
        this.smsService = smsService;
        this.userService = userService;
    }

    @Override
    public void sendLoginCode(String phone) {
        ensureEnabled();
        String normalizedPhone = normalize(phone);
        String code = StrUtil.isBlank(properties.getFixedCode())
                ? RandomUtil.randomNumbers(properties.getCodeLength()) : properties.getFixedCode();
        String key = key(normalizedPhone);
        SmsVerificationCodeStore.IssueResult issueResult = codeStore.issue(key, hash(code),
                properties.getTtl(), properties.getResendInterval(), properties.getMaxAttempts());
        if (issueResult == SmsVerificationCodeStore.IssueResult.TOO_FREQUENT) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        UserDO user = userService.getByPhone(normalizedPhone);
        if (user == null || !Integer.valueOf(0).equals(user.getStatus())) {
            return;
        }
        try {
            smsService.orElseThrow(() -> new BusinessException("短信功能未启用"))
                    .sendByTemplateCode(normalizedPhone, properties.getTemplateCode(), Map.of("code", code));
        } catch (RuntimeException exception) {
            codeStore.invalidate(key);
            throw exception;
        }
    }

    @Override
    public UserDO verifyLoginCode(String phone, String code) {
        ensureEnabled();
        String normalizedPhone = normalize(phone);
        SmsVerificationCodeStore.VerifyResult result =
                codeStore.verifyAndConsume(key(normalizedPhone), hash(code));
        if (result != SmsVerificationCodeStore.VerifyResult.SUCCESS) {
            throw new BusinessException(switch (result) {
                case EXPIRED -> "验证码已过期，请重新获取";
                case LOCKED -> "验证码错误次数过多，请重新获取";
                default -> "验证码错误";
            });
        }
        UserDO user = userService.getByPhone(normalizedPhone);
        if (user == null) {
            throw new BusinessException("验证码错误");
        }
        return user;
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException("短信验证码登录未启用");
        }
    }

    private String key(String phone) {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) {
            throw new BusinessException("缺少租户上下文");
        }
        return tenantId + ":" + phone;
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private String hash(String code) {
        return DigestUtil.sha256Hex(code);
    }
}
