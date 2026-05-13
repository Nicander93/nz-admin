package com.nz.admin.framework.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 文件安全校验与文件名标准化。
 */
public class FileSecurityValidator {

    private final FileStorageProperties properties;

    public FileSecurityValidator(FileStorageProperties properties) {
        this.properties = properties;
    }

    public void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String extension = FileUtil.extName(originalFilename).toLowerCase(Locale.ROOT);
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型：" + extension);
        }
        String contentType = file.getContentType();
        if (StrUtil.isBlank(contentType) || !properties.getAllowedMimeTypes().contains(contentType)) {
            throw new IllegalArgumentException("MIME 类型不在白名单中：" + contentType);
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException("文件大小超过限制（字节）：" + properties.getMaxFileSizeBytes());
        }
    }

    public String normalizeOriginalFilename(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return "unknown";
        }
        String fileName = originalFilename.replace("\\", "/");
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return StrUtil.subPre(fileName, 255);
    }
}
