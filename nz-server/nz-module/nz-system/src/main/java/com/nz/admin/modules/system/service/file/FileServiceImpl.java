package com.nz.admin.modules.system.service.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.framework.file.FileStorageObject;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileStorageService;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.mapper.file.FileMapper;
import com.nz.admin.modules.system.entity.query.file.FileQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传服务实现。
 */
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    @Qualifier("localFileStorageService")
    private FileStorageService localFileStorageService;

    @Autowired
    @Qualifier("ossFileStorageService")
    private FileStorageService ossFileStorageService;

    @Autowired
    private FileStorageProperties storageProperties;

    @Override
    public Page<FileDO> listPage(FileQuery query) {
        return fileMapper.selectPageByCondition(query.toPage(), query);
    }

    @Override
    public String getStorageType() {
        return storageProperties.getStorageType() != null ? 
               storageProperties.getStorageType() : "local";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileDO upload(MultipartFile file, Long uploaderId) throws IOException {
        FileStorageService storageService = resolveByConfiguredType();
        FileDO sysFile = toFileDO(storageService.upload(file, uploaderId));
        sysFile.setStorageType(getStorageType());

        try {
            fileMapper.insert(sysFile);
            String fileUrl = storageService.getFileUrl(sysFile.getId());
            sysFile.setFileUrl(fileUrl);
            
            FileDO update = new FileDO();
            update.setId(sysFile.getId());
            update.setFileUrl(fileUrl);
            fileMapper.updateById(update);
            return sysFile;
        } catch (RuntimeException ex) {
            storageService.delete(toStorageObject(sysFile));
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
        FileDO sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            return;
        }
        resolveByFileRecord(sysFile).delete(toStorageObject(sysFile));
        fileMapper.deleteById(id);
    }

    @Override
    public void downloadById(Long id, HttpServletResponse response) throws IOException {
        FileDO sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        resolveByFileRecord(sysFile).download(toStorageObject(sysFile), response);
    }

    private FileStorageService resolveByConfiguredType() {
        return resolveByType(getStorageType());
    }

    private FileStorageService resolveByFileRecord(FileDO sysFile) {
        return resolveByType(sysFile != null ? sysFile.getStorageType() : null);
    }

    private FileStorageService resolveByType(String type) {
        return "oss".equalsIgnoreCase(type) ? ossFileStorageService : localFileStorageService;
    }

    private FileDO toFileDO(FileStorageObject storageObject) {
        FileDO sysFile = new FileDO();
        sysFile.setOriginalName(storageObject.getOriginalName());
        sysFile.setFileName(storageObject.getFileName());
        sysFile.setFilePath(storageObject.getFilePath());
        sysFile.setFileSize(storageObject.getFileSize());
        sysFile.setFileExt(storageObject.getFileExt());
        sysFile.setMimeType(storageObject.getMimeType());
        sysFile.setUploaderId(storageObject.getUploaderId());
        return sysFile;
    }

    private FileStorageObject toStorageObject(FileDO sysFile) {
        if (sysFile == null) {
            return null;
        }
        return new FileStorageObject()
                .setOriginalName(sysFile.getOriginalName())
                .setFileName(sysFile.getFileName())
                .setFilePath(sysFile.getFilePath())
                .setFileSize(sysFile.getFileSize())
                .setFileExt(sysFile.getFileExt())
                .setMimeType(sysFile.getMimeType())
                .setUploaderId(sysFile.getUploaderId());
    }
}
