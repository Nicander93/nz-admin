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
import java.util.function.Supplier;
import com.aliyun.oss.model.OSSObject;

/**
 * 阿里云 OSS 对象存储实现。
 */
public class OssFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties properties;
    private final FileSecurityValidator fileSecurityValidator;
    private final Supplier<OSS> clientSupplier;

    public OssFileStorageServiceImpl(FileStorageProperties properties, FileSecurityValidator validator) {
        this(properties, validator, () -> new OSSClientBuilder().build(properties.getOss().getEndpoint(),
                properties.getOss().getAccessKeyId(), properties.getOss().getAccessKeySecret()));
    }

    OssFileStorageServiceImpl(FileStorageProperties properties, FileSecurityValidator validator,
                              Supplier<OSS> clientSupplier) {
        this.properties = properties;
        this.fileSecurityValidator = validator;
        this.clientSupplier = clientSupplier;
    }

    private OSS getOssClient() { return clientSupplier.get(); }

    @Override
    public FileStorageObject upload(MultipartFile file, Long uploaderId) throws IOException {
        FileStorageObject target = prepareUpload(file, uploaderId);
        writeUpload(file, target);
        return target;
    }

    @Override
    public FileStorageObject prepareUpload(MultipartFile file, Long uploaderId) {
        fileSecurityValidator.validateUpload(file);
        String originalName = fileSecurityValidator.normalizeOriginalFilename(file.getOriginalFilename());
        String fileExt = FileUtil.extName(originalName);
        String prefix = StrUtil.blankToDefault(properties.getOss().getPathPrefix(), "");
        if (StrUtil.isNotBlank(prefix) && !prefix.endsWith("/")) prefix += "/";
        String fileName = prefix + IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");
        String filePath = "oss://" + properties.getOss().getBucketName() + "/" + fileName;
        return new FileStorageObject()
                .setOriginalName(originalName).setFileName(fileName).setFilePath(filePath)
                .setFileSize(file.getSize()).setFileExt(fileExt)
                .setMimeType(file.getContentType()).setUploaderId(uploaderId);
    }

    @Override
    public void writeUpload(MultipartFile file, FileStorageObject target) throws IOException {
        fileSecurityValidator.validateUpload(file);
        OSS client = getOssClient();
        try (var input = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            PutObjectRequest request = new PutObjectRequest(
                    properties.getOss().getBucketName(), target.getFileName(), input);
            request.setMetadata(metadata);
            client.putObject(request);
        } finally {
            client.shutdown();
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

        OSS client = getOssClient();
        try (OSSObject object = client.getObject(properties.getOss().getBucketName(), file.getFileName())) {
            FileDownloads.prepareResponse(response, file);
            object.getObjectContent().transferTo(response.getOutputStream());
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String getFileUrl(Long fileId) {
        if (StrUtil.isNotBlank(properties.getOss().getDomain())) {
            return StrUtil.removeSuffix(properties.getOss().getDomain(), "/") + "/" + fileId;
        }
        return "/api/system/file/download/" + fileId;
    }

    @Override
    public void checkAvailable() {
        OSS ossClient = getOssClient();
        try {
            if (!ossClient.doesBucketExist(properties.getOss().getBucketName())) {
                throw new IllegalStateException("OSS bucket 不存在: " + properties.getOss().getBucketName());
            }
        } finally {
            ossClient.shutdown();
        }
    }
}
