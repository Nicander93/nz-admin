package com.nz.admin.framework.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储实现。
 */
public class OssFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties properties;
    private final FileSecurityValidator fileSecurityValidator;

    public OssFileStorageServiceImpl(FileStorageProperties properties, FileSecurityValidator fileSecurityValidator) {
        this.properties = properties;
        this.fileSecurityValidator = fileSecurityValidator;
    }

    private OSS getOssClient() {
        return new OSSClientBuilder().build(
                properties.getOss().getEndpoint(),
                properties.getOss().getAccessKeyId(),
                properties.getOss().getAccessKeySecret()
        );
    }

    @Override
    public FileStorageObject upload(MultipartFile file, Long uploaderId) throws IOException {
        fileSecurityValidator.validateUpload(file);
        String originalName = fileSecurityValidator.normalizeOriginalFilename(file.getOriginalFilename());
        String fileExt = FileUtil.extName(originalName);
        String prefix = StrUtil.blankToDefault(properties.getOss().getPathPrefix(), "");
        if (StrUtil.isNotBlank(prefix) && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String fileName = prefix + IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");

        OSS ossClient = getOssClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            PutObjectRequest request = new PutObjectRequest(
                    properties.getOss().getBucketName(),
                    fileName,
                    file.getInputStream()
            );
            request.setMetadata(metadata);
            ossClient.putObject(request);

            return new FileStorageObject()
                    .setOriginalName(originalName)
                    .setFileName(fileName)
                    .setFilePath("oss://" + properties.getOss().getBucketName() + "/" + fileName)
                    .setFileSize(file.getSize())
                    .setFileExt(fileExt)
                    .setMimeType(file.getContentType())
                    .setUploaderId(uploaderId);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void delete(FileStorageObject file) {
        if (file == null || StrUtil.isBlank(file.getFileName())) {
            return;
        }
        OSS ossClient = getOssClient();
        try {
            ossClient.deleteObject(properties.getOss().getBucketName(), file.getFileName());
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void download(FileStorageObject file, HttpServletResponse response) throws IOException {
        if (file == null || StrUtil.isBlank(file.getFileName())) {
            response.setStatus(404);
            return;
        }

        OSS ossClient = getOssClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
            URL url = ossClient.generatePresignedUrl(properties.getOss().getBucketName(), file.getFileName(), expiration);
            response.sendRedirect(url.toString());
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getFileUrl(Long fileId) {
        if (StrUtil.isNotBlank(properties.getOss().getDomain())) {
            return StrUtil.removeSuffix(properties.getOss().getDomain(), "/") + "/" + fileId;
        }
        return "/api/system/file/download/" + fileId;
    }
}
