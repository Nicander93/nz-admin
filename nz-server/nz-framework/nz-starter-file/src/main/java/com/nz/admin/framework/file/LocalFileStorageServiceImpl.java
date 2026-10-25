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
import java.nio.file.Files;
import java.nio.file.Path;

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
        if (file == null || StrUtil.isBlank(file.getFilePath())) {
            return;
        }

        try {
            Path filePath = resolveDeletePath(file.getFilePath());
            if (Files.isDirectory(filePath)) {
                throw new IllegalArgumentException("不能删除目录: " + file.getFilePath());
            }
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("删除本地文件失败: " + file.getFilePath(), exception);
        }
    }

    @Override
    public void download(FileStorageObject fileObject, HttpServletResponse response) throws IOException {
        if (fileObject == null || StrUtil.isBlank(fileObject.getFilePath())) {
            response.setStatus(404);
            return;
        }

        Path canonicalPath = resolveCanonicalPathWithinBase(fileObject.getFilePath(), true);
        File file = canonicalPath.toFile();

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

    private Path resolveDeletePath(String filePath) throws IOException {
        Path inputPath = FileUtil.file(filePath).toPath().toAbsolutePath().normalize();
        Path canonicalPath = resolveCanonicalPathWithinBase(inputPath.toString(), false);
        if (inputPath.getFileName() == null || inputPath.getParent() == null) {
            throw new IllegalArgumentException("非法文件路径");
        }

        Path canonicalParent = inputPath.getParent().toFile().getCanonicalFile().toPath();
        Path deletePath = canonicalParent.resolve(inputPath.getFileName());
        Path canonicalBase = FileUtil.file(properties.getBasePath()).getCanonicalFile().toPath();
        if (!deletePath.startsWith(canonicalBase) || Files.isDirectory(canonicalPath)) {
            throw new IllegalArgumentException("不能删除目录或非法文件路径: " + filePath);
        }
        return deletePath;
    }

    private Path resolveCanonicalPathWithinBase(String filePath, boolean allowBasePath) throws IOException {
        Path basePath = FileUtil.file(properties.getBasePath()).getCanonicalFile().toPath();
        Path canonicalPath = FileUtil.file(filePath).getCanonicalFile().toPath();
        if (!canonicalPath.startsWith(basePath)
                || (!allowBasePath && canonicalPath.equals(basePath))) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return canonicalPath;
    }

    @Override
    public String getFileUrl(Long fileId) {
        return properties.getLocalAccessUrlPrefix() + fileId;
    }

    @Override
    public void checkAvailable() {
        File directory = FileUtil.mkdir(properties.getBasePath());
        if (!directory.isDirectory() || !directory.canWrite()) {
            throw new IllegalStateException("本地存储目录不可写: " + properties.getBasePath());
        }
    }
}
