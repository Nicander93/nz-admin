package com.nz.admin.framework.encryption.core;

/**
 * 未启用字段加密时的透传实现。
 */
public final class NoopFieldCipher implements FieldCipher {

    public static final NoopFieldCipher INSTANCE = new NoopFieldCipher();

    private NoopFieldCipher() {
    }

    @Override
    public String encrypt(String value) {
        return value;
    }

    @Override
    public String decrypt(String value) {
        return value;
    }

    @Override
    public boolean isEncrypted(String value) {
        return false;
    }

    @Override
    public String activeKeyId() {
        return "disabled";
    }
}
