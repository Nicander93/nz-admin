package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.system.config.SysFileStorageProperties;
import com.nz.admin.modules.system.entity.po.SysFileDO;
import com.nz.admin.modules.system.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 本地文件存储实现。
 */
@Service("localFileStorageService")
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Autowired
    private SysFileStorageProperties properties;

    @Override
    public SysFileDO upload(MultipartFile file, Long uploaderId) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileExt = FileUtil.extName(originalName);
        String fileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");
        String filePath = properties.getBasePath() + "/" + fileName;

        File dest = new File(filePath);
        FileUtil.mkParentDirs(dest);
        file.transferTo(dest);

        SysFileDO sysFile = new SysFileDO();
        sysFile.setOriginalName(originalName);
        sysFile.setFileName(fileName);
        sysFile.setFilePath(filePath);
        sysFile.setFileSize(file.getSize());
        sysFile.setFileExt(fileExt);
        sysFile.setMimeType(file.getContentType());
        sysFile.setUploaderId(uploaderId);
        return sysFile;
    }

    @Override
    public void delete(SysFileDO sysFile) {
        if (sysFile != null && StrUtil.isNotBlank(sysFile.getFilePath())) {
            FileUtil.del(sysFile.getFilePath());
        }
    }

    @Override
    public void download(SysFileDO sysFile, HttpServletResponse response) throws IOException {
        if (sysFile == null || StrUtil.isBlank(sysFile.getFilePath())) {
            response.setStatus(404);
            return;
        }

        File baseDir = FileUtil.file(properties.getBasePath());
        String baseCanonicalPath = baseDir.getCanonicalPath();
        String expectedPrefix = baseCanonicalPath + File.separator;

        File file = FileUtil.file(sysFile.getFilePath());
        String fileCanonicalPath = file.getCanonicalPath();
        if (!StrUtil.startWith(fileCanonicalPath, expectedPrefix)
                && !StrUtil.equals(fileCanonicalPath, baseCanonicalPath)) {
            throw new IllegalArgumentException("非法文件路径");
        }

        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            return;
        }

        String downloadName = StrUtil.blankToDefault(sysFile.getOriginalName(), sysFile.getFileName());
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(file.length());
        FileUtil.writeToStream(file, response.getOutputStream());
    }

    @Override
    public String getFileUrl(Long fileId) {
        return "/api/system/file/download/" + fileId;
    }
}
