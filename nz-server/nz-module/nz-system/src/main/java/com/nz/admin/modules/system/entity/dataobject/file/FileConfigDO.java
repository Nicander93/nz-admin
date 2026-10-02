package com.nz.admin.modules.system.entity.dataobject.file;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/** 文件存储配置实体。 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_config")
public class FileConfigDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configName;
    private String storageType;
    private String basePath;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String region;
    private String domain;
    private String pathPrefix;
    private String localAccessUrlPrefix;
    private Long maxFileSizeBytes;
    private Integer status;
    private String remark;
}
