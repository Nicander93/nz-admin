package com.nz.admin.framework.encryption.core;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.encryption.config.FieldEncryptionProperties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AES-256-GCM 字段加密实现。
 *
 * <p>密文格式为 ENC$1$keyId$base64url(iv + ciphertext)。</p>
 */
public class AesGcmFieldCipher implements FieldCipher {

    private static final String PREFIX = "ENC$1$";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final String activeKeyId;
    private final boolean allowPlaintextRead;
    private final Map<String, SecretKeySpec> keys;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmFieldCipher(FieldEncryptionProperties properties) {
        this.activeKeyId = properties.getActiveKeyId();
        this.allowPlaintextRead = properties.isAllowPlaintextRead();
        this.keys = buildKeys(properties.getKeys());
        if (StrUtil.isBlank(activeKeyId) || !keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("活动字段加密密钥不存在: " + activeKeyId);
        }
    }

    @Override
    public String encrypt(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }
        String plaintext = isEncrypted(value) ? decrypt(value) : value;
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_LENGTH, iv));
            cipher.updateAAD(aad(activeKeyId));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + activeKeyId + "$"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("敏感字段加密失败", exception);
        }
    }

    @Override
    public String decrypt(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }
        if (!isEncrypted(value)) {
            if (value.startsWith("ENC$")) {
                throw new IllegalStateException("敏感字段密文格式不正确");
            }
            if (allowPlaintextRead) {
                return value;
            }
            throw new IllegalStateException("发现未加密的敏感字段");
        }
        String[] parts = value.split("\\$", 4);
        String keyId = parts[2];
        SecretKeySpec key = keys.get(keyId);
        if (key == null) {
            throw new IllegalStateException("找不到密文对应的字段加密密钥: " + keyId);
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[3]);
            if (payload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文载荷长度不正确");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            cipher.updateAAD(aad(keyId));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("敏感字段解密失败", exception);
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = value.split("\\$", 4);
        return parts.length == 4 && StrUtil.isNotBlank(parts[2]) && StrUtil.isNotBlank(parts[3]);
    }

    @Override
    public String activeKeyId() {
        return activeKeyId;
    }

    private Map<String, SecretKeySpec> buildKeys(Map<String, String> configuredKeys) {
        if (configuredKeys == null || configuredKeys.isEmpty()) {
            throw new IllegalArgumentException("启用字段加密时至少需要配置一个密钥");
        }
        Map<String, SecretKeySpec> result = new LinkedHashMap<>();
        configuredKeys.forEach((keyId, material) -> {
            if (StrUtil.isBlank(keyId) || StrUtil.isBlank(material) || material.length() < 16) {
                throw new IllegalArgumentException("字段加密 keyId 不能为空，密钥材料至少 16 个字符");
            }
            result.put(keyId, new SecretKeySpec(sha256(material), "AES"));
        });
        return result;
    }

    private byte[] sha256(String material) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法初始化字段加密密钥", exception);
        }
    }

    private byte[] aad(String keyId) {
        return ("1:" + keyId).getBytes(StandardCharsets.UTF_8);
    }
}
