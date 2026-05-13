package com.nz.admin.modules.system.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.file.FileSecurityValidator;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.query.file.FileQuery;
import com.nz.admin.modules.system.service.file.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传管理接口（支持本地/OSS 存储）。
 */
@RestController
@RequestMapping("/api/system/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private LoginUserContext loginUserContext;
    @Autowired
    private FileSecurityValidator fileSecurityValidator;

    /**
     * 单文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/upload")
    public R<FileDO> upload(@RequestParam("file") MultipartFile file) {
        try {
            Long uploaderId = loginUserContext.getLoginUserIdOrNull();
            return R.ok(fileService.upload(file, uploaderId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (IOException e) {
            return R.fail("上传失败：" + e.getMessage());
        } catch (RuntimeException e) {
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 多文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/uploads")
    public R<FileService.BatchUploadResult> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return R.fail("文件不能为空");
            }
            for (MultipartFile file : files) {
                fileSecurityValidator.validateUpload(file);
            }
            Long uploaderId = loginUserContext.getLoginUserIdOrNull();
            return R.ok(fileService.uploadBatch(files, uploaderId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (RuntimeException e) {
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询文件列表。
     */
    @SaCheckPermission("system:file:list")
    @GetMapping("/page")
    public R<PageResult<FileDO>> page(FileQuery query) {
        return R.ok(PageResult.of(fileService.listPage(query)));
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
}
