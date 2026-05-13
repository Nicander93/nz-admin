package com.nz.admin.framework.file;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文件存储层返回的中立对象，不绑定具体业务表。
 */
@Data
@Accessors(chain = true)
public class FileStorageObject {

    private String originalName;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileExt;

    private String mimeType;

    private Long uploaderId;
}
