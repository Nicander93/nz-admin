package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储策略接口。
 */
public interface FileStorageService {

    /**
     * 上传文件。
     */
    SysFile upload(MultipartFile file, Long uploaderId) throws IOException;

    /**
     * 删除文件。
     */
    void delete(SysFile sysFile);

    /**
     * 下载文件。
     */
    void download(SysFile sysFile, HttpServletResponse response) throws IOException;

    /**
     * 获取文件访问 URL。
     */
    String getFileUrl(Long fileId);
}
