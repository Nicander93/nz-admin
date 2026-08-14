package com.nz.admin.modules.system.entity.vo.file;
import lombok.Data;
import java.time.LocalDateTime;

/** 文件存储配置视图，不返回密钥原文。 */
@Data
public class FileConfigVO {
    private Long id;
    private String configName;
    private String storageType;
    private String basePath;
    private String endpoint;
    private String accessKeyIdMasked;
    private boolean accessKeySecretConfigured;
    private String bucketName;
    private String region;
    private String domain;
    private String pathPrefix;
    private String localAccessUrlPrefix;
    private Long maxFileSizeBytes;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
