package com.nz.admin.framework.encryption.mask;

import cn.hutool.core.util.StrUtil;

/**
 * 常用敏感信息脱敏工具。
 */
public final class SensitiveDataUtils {

    private SensitiveDataUtils() {
    }

    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        if (phone.length() <= 7) {
            return "*".repeat(phone.length());
        }
        return StrUtil.subPre(phone, 3)
                + "*".repeat(phone.length() - 7)
                + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        int separator = email.indexOf('@');
        if (separator <= 0) {
            return maskGeneric(email);
        }
        return email.charAt(0) + "****" + email.substring(separator);
    }

    private static String maskGeneric(String value) {
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.charAt(0)
                + "*".repeat(value.length() - 2)
                + value.charAt(value.length() - 1);
    }
}
