package com.nz.admin.framework.encryption.core;

import java.util.Objects;

/**
 * 为 MyBatis 创建的 TypeHandler 提供当前字段加密器。
 */
public final class FieldCipherHolder {

    private static volatile FieldCipher cipher = NoopFieldCipher.INSTANCE;

    private FieldCipherHolder() {
    }

    public static FieldCipher get() {
        return cipher;
    }

    public static void install(FieldCipher fieldCipher) {
        cipher = Objects.requireNonNull(fieldCipher);
    }

    public static void clear(FieldCipher expected) {
        if (cipher == expected) {
            cipher = NoopFieldCipher.INSTANCE;
        }
    }
}
