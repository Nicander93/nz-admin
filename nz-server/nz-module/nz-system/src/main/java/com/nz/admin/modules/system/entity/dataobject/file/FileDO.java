package com.nz.admin.modules.system.entity.dataobject.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nz.admin.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 文件上传记录实体。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class FileDO extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @JsonIgnore
    private Long tenantId;

    /** 上传时的位置与密文凭据快照，不随当前配置变更。 */
    @JsonIgnore
    private String storageConfigSnapshot;

    private String lifecycleStatus;
    private LocalDateTime cleanupAfter;
    private Integer cleanupAttempts;
    private String cleanupError;

    public boolean isStorageBindingRequired() {
        return storageConfigSnapshot == null || storageConfigSnapshot.isBlank();
    }

    /**
     * 原始文件名。
     */
    private String originalName;

    /**
     * 存储文件名（重命名后）。
     */
    private String fileName;

    /**
     * 本地绝对路径或 OSS key。
     */
    private String filePath;

    /**
     * 文件下载地址。
     */
    private String fileUrl;

    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;

    /**
     * 文件后缀。
     */
    private String fileExt;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 上传人 id。
     */
    private Long uploaderId;

    /**
     * 上传人名称。
     */
    private String uploaderName;

    /**
     * 存储类型：local 或 oss。
     */
    private String storageType;
}
