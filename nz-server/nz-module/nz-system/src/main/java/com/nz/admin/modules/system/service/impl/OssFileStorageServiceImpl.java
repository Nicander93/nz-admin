package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.nz.admin.modules.system.config.SysFileStorageProperties;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储实现。
 */
@Service("ossFileStorageService")
public class OssFileStorageServiceImpl implements FileStorageService {

    @Autowired
    private SysFileStorageProperties properties;

    private OSS getOssClient() {
        return new OSSClientBuilder().build(
                properties.getOss().getEndpoint(),
                properties.getOss().getAccessKeyId(),
                properties.getOss().getAccessKeySecret()
        );
    }

    @Override
    public SysFile upload(MultipartFile file, Long uploaderId) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileExt = FileUtil.extName(originalName);
        String fileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");

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

            SysFile sysFile = new SysFile();
            sysFile.setOriginalName(originalName);
            sysFile.setFileName(fileName);
            sysFile.setFilePath("oss://" + properties.getOss().getBucketName() + "/" + fileName);
            sysFile.setFileSize(file.getSize());
            sysFile.setFileExt(fileExt);
            sysFile.setMimeType(file.getContentType());
            sysFile.setUploaderId(uploaderId);
            return sysFile;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void delete(SysFile sysFile) {
        if (sysFile == null || StrUtil.isBlank(sysFile.getFileName())) {
            return;
        }
        OSS ossClient = getOssClient();
        try {
            ossClient.deleteObject(properties.getOss().getBucketName(), sysFile.getFileName());
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void download(SysFile sysFile, HttpServletResponse response) throws IOException {
        if (sysFile == null || StrUtil.isBlank(sysFile.getFileName())) {
            response.setStatus(404);
            return;
        }

        OSS ossClient = getOssClient();
        try {
            // 生成签名 URL（有效期 5 分钟）
            Date expiration = new Date(System.currentTimeMillis() + 5 * 60 * 1000);
            URL url = ossClient.generatePresignedUrl(properties.getOss().getBucketName(), sysFile.getFileName(), expiration);
            response.sendRedirect(url.toString());
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String getFileUrl(Long fileId) {
        return "/api/system/file/download/" + fileId;
    }
}
