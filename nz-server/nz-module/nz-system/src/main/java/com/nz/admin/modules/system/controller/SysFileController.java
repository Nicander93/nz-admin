package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.query.SysFileQuery;
import com.nz.admin.modules.system.service.SysFileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传管理接口（支持本地/OSS 存储）。
 */
@RestController
@RequestMapping("/api/system/file")
public class SysFileController {

    // 白名单：允许上传的文件扩展名及其 MIME 类型
    private static final Map<String, Set<String>> ALLOWED_FILE_TYPES = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg", "application/octet-stream")),
            Map.entry("jpeg", Set.of("image/jpeg", "application/octet-stream")),
            Map.entry("png", Set.of("image/png", "application/octet-stream")),
            Map.entry("gif", Set.of("image/gif", "application/octet-stream")),
            Map.entry("pdf", Set.of("application/pdf", "application/octet-stream")),
            Map.entry("txt", Set.of("text/plain", "application/octet-stream")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream"))
    );

    @Autowired
    private SysFileService fileService;

    /**
     * 单文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/upload")
    public R<SysFile> upload(@RequestParam("file") MultipartFile file) {
        try {
            validateFile(file);
            Long uploaderId = StpUtil.getLoginIdAsLong();
            return R.ok(fileService.upload(file, uploaderId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (OSSException | ClientException e) {
            return R.fail("OSS 上传失败：" + e.getMessage());
        } catch (IOException e) {
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 多文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/uploads")
    public R<SysFileService.BatchUploadResult> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return R.fail("文件不能为空");
            }
            validateFiles(files);
            Long uploaderId = StpUtil.getLoginIdAsLong();
            return R.ok(fileService.uploadBatch(files, uploaderId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (OSSException | ClientException e) {
            return R.fail("OSS 上传失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询文件列表。
     */
    @SaCheckPermission("system:file:list")
    @GetMapping("/page")
    public R<Page<SysFile>> page(SysFileQuery query) {
        return R.ok(fileService.listPage(query));
    }

    /**
     * 查看当前文件存储类型。
     */
    @SaCheckPermission("system:file:storage-type")
    @GetMapping("/storage-type")
    public R<String> getStorageType() {
        return R.ok(fileService.getStorageType());
    }

    /**
     * 下载文件。
     */
    @SaCheckPermission("system:file:download")
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileService.downloadById(id, response);
    }

    /**
     * 删除文件。
     */
    @SaCheckPermission("system:file:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.removeById(id);
        return R.ok();
    }

    /**
     * 文件上传参数校验（白名单）。
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String fileExt = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_FILE_TYPES.containsKey(fileExt)) {
            throw new IllegalArgumentException("不支持的文件类型：" + fileExt);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.get(fileExt).contains(contentType)) {
            throw new IllegalArgumentException("MIME 类型不在白名单中：" + contentType);
        }

        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过 100MB");
        }
    }

    private void validateFiles(MultipartFile[] files) {
        if (files == null) {
            throw new IllegalArgumentException("文件不能为空");
        }
        for (MultipartFile file : files) {
            validateFile(file);
        }
    }
}
