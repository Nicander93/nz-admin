package com.nz.admin.framework.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * S3 兼容对象存储实现，默认用于 MinIO。
 */
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties properties;
    private final FileSecurityValidator fileSecurityValidator;
    private final Supplier<MinioClient> clientSupplier;

    public MinioFileStorageServiceImpl(FileStorageProperties properties,
                                       FileSecurityValidator fileSecurityValidator) {
        this(properties, fileSecurityValidator, () -> MinioClient.builder()
                .endpoint(properties.getS3().getEndpoint())
                .credentials(properties.getS3().getAccessKeyId(), properties.getS3().getAccessKeySecret())
                .build());
    }

    MinioFileStorageServiceImpl(FileStorageProperties properties,
                                FileSecurityValidator fileSecurityValidator,
                                Supplier<MinioClient> clientSupplier) {
        this.properties = properties;
        this.fileSecurityValidator = fileSecurityValidator;
        this.clientSupplier = clientSupplier;
    }

    @Override
    public FileStorageObject upload(MultipartFile file, Long uploaderId) throws IOException {
        fileSecurityValidator.validateUpload(file);
        String originalName = fileSecurityValidator.normalizeOriginalFilename(file.getOriginalFilename());
        String fileExt = FileUtil.extName(originalName);
        String prefix = normalizePrefix(properties.getS3().getPathPrefix());
        String fileName = prefix + IdUtil.fastSimpleUUID()
                + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");

        try {
            clientSupplier.get().putObject(PutObjectArgs.builder()
                    .bucket(properties.getS3().getBucketName())
                    .object(fileName)
                    .contentType(file.getContentType())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
            return new FileStorageObject()
                    .setOriginalName(originalName)
                    .setFileName(fileName)
                    .setFilePath("s3://" + properties.getS3().getBucketName() + "/" + fileName)
                    .setFileSize(file.getSize())
                    .setFileExt(fileExt)
                    .setMimeType(file.getContentType())
                    .setUploaderId(uploaderId);
        } catch (Exception e) {
            throw new IOException("上传到 S3 兼容存储失败", e);
        }
    }

    @Override
    public void delete(FileStorageObject file) {
        if (file == null || StrUtil.isBlank(file.getFileName())) {
            return;
        }
        try {
            clientSupplier.get().removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getS3().getBucketName())
                    .object(file.getFileName())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("删除 S3 兼容存储文件失败", e);
        }
    }

    @Override
    public void download(FileStorageObject file, HttpServletResponse response) throws IOException {
        if (file == null || StrUtil.isBlank(file.getFileName())) {
            response.setStatus(404);
            return;
        }
        try {
            String url = clientSupplier.get().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getS3().getBucketName())
                    .object(file.getFileName())
                    .expiry(5, TimeUnit.MINUTES)
                    .build());
            response.sendRedirect(url);
        } catch (Exception e) {
            throw new IOException("生成 S3 兼容存储下载地址失败", e);
        }
    }

    @Override
    public String getFileUrl(Long fileId) {
        if (StrUtil.isNotBlank(properties.getS3().getDomain())) {
            return StrUtil.removeSuffix(properties.getS3().getDomain(), "/") + "/" + fileId;
        }
        return "/api/system/file/download/" + fileId;
    }

    @Override
    public void checkAvailable() {
        try {
            boolean exists = clientSupplier.get().bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getS3().getBucketName())
                    .build());
            if (!exists) {
                throw new IllegalStateException("S3 bucket 不存在: " + properties.getS3().getBucketName());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("S3 兼容存储连接失败", e);
        }
    }

    private String normalizePrefix(String configuredPrefix) {
        String prefix = StrUtil.blankToDefault(configuredPrefix, "");
        return StrUtil.isNotBlank(prefix) && !prefix.endsWith("/") ? prefix + "/" : prefix;
    }
}
