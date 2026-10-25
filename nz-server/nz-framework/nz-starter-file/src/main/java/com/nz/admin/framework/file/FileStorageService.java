package com.nz.admin.framework.file;

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
    FileStorageObject upload(MultipartFile file, Long uploaderId) throws IOException;

    /** 预先分配目标位置，业务层可在写入前持久化补偿信息。 */
    default FileStorageObject prepareUpload(MultipartFile file, Long uploaderId) throws IOException {
        throw new UnsupportedOperationException("存储适配器尚未实现可恢复上传");
    }

    /** 写入已分配的位置；失败后仍可按同一位置重试删除。 */
    default void writeUpload(MultipartFile file, FileStorageObject target) throws IOException {
        throw new UnsupportedOperationException("存储适配器尚未实现可恢复上传");
    }

    /**
     * 删除文件。
     */
    void delete(FileStorageObject file);

    /**
     * 下载文件。
     */
    void download(FileStorageObject file, HttpServletResponse response) throws IOException;

    /**
     * 获取文件访问 URL。
     */
    String getFileUrl(Long fileId);

    /**
     * 检查当前存储配置和目标位置是否可用。
     */
    void checkAvailable();
}
