package com.nz.admin.modules.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "nz.file")
public class SysFileStorageProperties {

    /**
     * 存储类型：local（本地）或 oss（对象存储）
     */
    private String storageType = "local";

    /**
     * 本地存储路径。
     */
    private String basePath = System.getProperty("user.home") + "/nz-admin/upload";

    // OSS 配置
    private Oss oss = new Oss();

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String domain; // 自定义域名（可选）
    }
}
