package com.nz.admin.framework.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 本地文件存储实现。
 */
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties properties;
    private final FileSecurityValidator fileSecurityValidator;

    public LocalFileStorageServiceImpl(FileStorageProperties properties, FileSecurityValidator fileSecurityValidator) {
        this.properties = properties;
        this.fileSecurityValidator = fileSecurityValidator;
    }

    @Override
    public FileStorageObject upload(MultipartFile file, Long uploaderId) throws IOException {
        fileSecurityValidator.validateUpload(file);
        String originalName = fileSecurityValidator.normalizeOriginalFilename(file.getOriginalFilename());
        String fileExt = FileUtil.extName(originalName);
        String fileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");
        String filePath = properties.getBasePath() + "/" + fileName;

        File dest = new File(filePath);
        FileUtil.mkParentDirs(dest);
        String baseCanonicalPath = new File(properties.getBasePath()).getCanonicalPath();
        String destCanonicalPath = dest.getCanonicalPath();
        if (!StrUtil.startWith(destCanonicalPath, baseCanonicalPath + File.separator)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        file.transferTo(dest);

        return new FileStorageObject()
                .setOriginalName(originalName)
                .setFileName(fileName)
                .setFilePath(filePath)
                .setFileSize(file.getSize())
                .setFileExt(fileExt)
                .setMimeType(file.getContentType())
                .setUploaderId(uploaderId);
    }

    @Override
    public void delete(FileStorageObject file) {
        if (file != null && StrUtil.isNotBlank(file.getFilePath())) {
            FileUtil.del(file.getFilePath());
        }
    }

    @Override
    public void download(FileStorageObject fileObject, HttpServletResponse response) throws IOException {
        if (fileObject == null || StrUtil.isBlank(fileObject.getFilePath())) {
            response.setStatus(404);
            return;
        }

        File baseDir = FileUtil.file(properties.getBasePath());
        String baseCanonicalPath = baseDir.getCanonicalPath();
        String expectedPrefix = baseCanonicalPath + File.separator;

        File file = FileUtil.file(fileObject.getFilePath());
        String fileCanonicalPath = file.getCanonicalPath();
        if (!StrUtil.startWith(fileCanonicalPath, expectedPrefix)
                && !StrUtil.equals(fileCanonicalPath, baseCanonicalPath)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            return;
        }

        String downloadName = StrUtil.blankToDefault(fileObject.getOriginalName(), fileObject.getFileName());
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.length());
        FileUtil.writeToStream(file, response.getOutputStream());
    }

    @Override
    public String getFileUrl(Long fileId) {
        return properties.getLocalAccessUrlPrefix() + fileId;
    }
}
