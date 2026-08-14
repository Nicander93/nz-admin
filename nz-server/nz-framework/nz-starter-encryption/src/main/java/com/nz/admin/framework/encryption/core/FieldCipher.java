package com.nz.admin.framework.encryption.core;

/**
 * 数据库敏感字段加解密协议。
 */
public interface FieldCipher {

    String encrypt(String value);

    String decrypt(String value);

    boolean isEncrypted(String value);

    String activeKeyId();
}
