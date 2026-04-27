package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.query.SysFileQuery;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传服务接口。
 */
public interface SysFileService {

    Page<SysFile> listPage(SysFileQuery query);

    String getStorageType();

    SysFile upload(MultipartFile file, Long uploaderId) throws IOException;

    BatchUploadResult uploadBatch(MultipartFile[] files, Long uploaderId);

    void removeById(Long id);

    void downloadById(Long id, HttpServletResponse response) throws IOException;

    /**
     * 批量上传结果。
     */
    public static class BatchUploadResult {
        private List<SysFile> successFiles = new ArrayList<>();
        private List<FailedFile> failedFiles = new ArrayList<>();
        private Integer totalCount = 0;
        private Integer successCount = 0;
        private Integer failCount = 0;

        public List<SysFile> getSuccessFiles() { return successFiles; }
        public void setSuccessFiles(List<SysFile> successFiles) { this.successFiles = successFiles; }
        public List<FailedFile> getFailedFiles() { return failedFiles; }
        public void setFailedFiles(List<FailedFile> failedFiles) { this.failedFiles = failedFiles; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        public Integer getFailCount() { return failCount; }
        public void setFailCount(Integer failCount) { this.failCount = failCount; }

        /**
         * 批量上传失败项。
         */
        public static class FailedFile {
            private String fileName;
            private String reason;

            public String getFileName() { return fileName; }
            public void setFileName(String fileName) { this.fileName = fileName; }
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
    }
}
