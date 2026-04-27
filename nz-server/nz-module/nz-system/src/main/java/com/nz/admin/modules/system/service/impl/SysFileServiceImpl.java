package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.config.SysFileStorageProperties;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.mapper.SysFileMapper;
import com.nz.admin.modules.system.query.SysFileQuery;
import com.nz.admin.modules.system.service.FileStorageService;
import com.nz.admin.modules.system.service.SysFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传服务实现。
 */
@Service
public class SysFileServiceImpl implements SysFileService {

    @Autowired
    private SysFileMapper fileMapper;

    @Autowired
    @Qualifier("localFileStorageService")
    private FileStorageService localFileStorageService;

    @Autowired
    @Qualifier("ossFileStorageService")
    private FileStorageService ossFileStorageService;

    @Autowired
    private SysFileStorageProperties storageProperties;

    @Override
    public Page<SysFile> listPage(SysFileQuery query) {
        return fileMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public String getStorageType() {
        return storageProperties.getStorageType() != null ? 
               storageProperties.getStorageType() : "local";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, Long uploaderId) throws IOException {
        FileStorageService storageService = resolveByConfiguredType();
        SysFile sysFile = storageService.upload(file, uploaderId);
        sysFile.setStorageType(getStorageType());

        try {
            fileMapper.insert(sysFile);
            String fileUrl = storageService.getFileUrl(sysFile.getId());
            sysFile.setFileUrl(fileUrl);
            
            SysFile update = new SysFile();
            update.setId(sysFile.getId());
            update.setFileUrl(fileUrl);
            fileMapper.updateById(update);
            return sysFile;
        } catch (RuntimeException ex) {
            storageService.delete(sysFile);
            throw ex;
        }
    }

    @Override
    public BatchUploadResult uploadBatch(MultipartFile[] files, Long uploaderId) {
        BatchUploadResult result = new BatchUploadResult();
        if (files == null) {
            return result;
        }

        result.setTotalCount(files.length);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                BatchUploadResult.FailedFile failedFile = new BatchUploadResult.FailedFile();
                failedFile.setFileName(file == null ? "" : file.getOriginalFilename());
                failedFile.setReason("文件不能为空");
                result.getFailedFiles().add(failedFile);
                continue;
            }

            try {
                result.getSuccessFiles().add(upload(file, uploaderId));
            } catch (Exception e) {
                BatchUploadResult.FailedFile failedFile = new BatchUploadResult.FailedFile();
                failedFile.setFileName(file.getOriginalFilename());
                failedFile.setReason(e.getMessage() != null ? e.getMessage() : "上传失败");
                result.getFailedFiles().add(failedFile);
            }
        }

        result.setSuccessCount(result.getSuccessFiles().size());
        result.setFailCount(result.getFailedFiles().size());
        return result;
    }

    @Override
    public void removeById(Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            return;
        }
        resolveByFileRecord(sysFile).delete(sysFile);
        fileMapper.deleteById(id);
    }

    @Override
    public void downloadById(Long id, HttpServletResponse response) throws IOException {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        resolveByFileRecord(sysFile).download(sysFile, response);
    }

    private FileStorageService resolveByConfiguredType() {
        return resolveByType(getStorageType());
    }

    private FileStorageService resolveByFileRecord(SysFile sysFile) {
        return resolveByType(sysFile != null ? sysFile.getStorageType() : null);
    }

    private FileStorageService resolveByType(String type) {
        return "oss".equalsIgnoreCase(type) ? ossFileStorageService : localFileStorageService;
    }
}
