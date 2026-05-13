package com.nz.admin.framework.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.file")
public class FileStorageProperties {

    /**
     * 存储类型：local 或 oss。
     */
    private String storageType = "local";

    /**
     * 本地存储路径。
     */
    private String basePath = System.getProperty("user.home") + "/nz-admin/upload";

    /**
     * 单文件最大字节数，默认 100MB。
     */
    private long maxFileSizeBytes = 100L * 1024 * 1024;

    /**
     * 允许上传的扩展名。
     */
    private List<String> allowedExtensions = new ArrayList<>(List.of(
            "jpg", "jpeg", "png", "gif", "pdf", "txt", "doc", "docx", "xls", "xlsx", "zip"
    ));

    /**
     * 允许上传的 MIME 类型。
     */
    private List<String> allowedMimeTypes = new ArrayList<>(List.of(
            "image/jpeg", "image/png", "image/gif", "application/pdf", "text/plain",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip", "application/x-zip-compressed", "application/octet-stream"
    ));

    /**
     * 本地文件访问前缀。
     */
    private String localAccessUrlPrefix = "/api/system/file/download/";

    private Oss oss = new Oss();

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String domain;
        private String pathPrefix = "";
    }
}
