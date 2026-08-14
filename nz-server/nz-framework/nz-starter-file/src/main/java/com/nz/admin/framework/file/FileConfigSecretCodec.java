package com.nz.admin.framework.file;

import cn.hutool.core.util.StrUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 文件配置敏感字段编解码器。
 */
public class FileConfigSecretCodec {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final FileStorageProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public FileConfigSecretCodec(FileStorageProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        if (StrUtil.isBlank(plaintext)) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("文件配置密钥加密失败", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (StrUtil.isBlank(ciphertext)) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("文件配置密钥解密失败", exception);
        }
    }

    private SecretKeySpec key() throws GeneralSecurityException {
        String configuredKey = properties.getConfigEncryptionKey();
        if (StrUtil.isBlank(configuredKey)) {
            throw new IllegalStateException("请配置 nz.file.config-encryption-key 后再保存 OSS 密钥");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
