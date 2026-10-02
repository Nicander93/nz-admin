package com.nz.admin.modules.system.entity.dto.file;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 文件存储配置保存参数。 */
@Data
public class FileConfigSaveRequest {
    private Long id;
    @NotBlank @Size(max = 100)
    private String configName;
    @NotBlank
    @Pattern(regexp = "local|oss|s3", message = "存储类型只能是 local、oss 或 s3")
    private String storageType;
    @Size(max = 500)
    private String basePath;
    @Size(max = 500)
    private String endpoint;
    @Size(max = 200)
    private String accessKeyId;
    @Size(max = 500)
    private String accessKeySecret;
    @Size(max = 200)
    private String bucketName;
    @Size(max = 100)
    private String region;
    @Size(max = 500)
    private String domain;
    @Size(max = 200)
    private String pathPrefix;
    @Size(max = 200)
    private String localAccessUrlPrefix;
    @Min(1)
    private Long maxFileSizeBytes;
    @Size(max = 500)
    private String remark;
}
