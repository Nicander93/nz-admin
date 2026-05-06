package com.nz.admin.modules.system.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.service.SysFileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传管理接口。
 */
@RestController
@RequestMapping("/api/system/file")
public class SysFileController {

    @Autowired
    private SysFileService fileService;

    @Value("${nz.file.base-path:${user.home}/nz-admin/upload}")
    private String basePath;

    /**
     * 单文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/upload")
    public R<SysFile> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        try {
            SysFile sysFile = saveFile(file);
            fileService.save(sysFile);
            return R.ok(sysFile);
        } catch (IOException e) {
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 多文件上传。
     */
    @SaCheckPermission("system:file:upload")
    @PostMapping("/uploads")
    public R<List<SysFile>> uploadMultiple(@RequestParam("files") MultipartFile[] files, HttpServletRequest request) {
        if (files == null || files.length == 0) {
            return R.fail("文件不能为空");
        }
        List<SysFile> savedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    SysFile sysFile = saveFile(file);
                    savedFiles.add(sysFile);
                }
            }
            fileService.saveBatch(savedFiles);
            return R.ok(savedFiles);
        } catch (IOException e) {
            return R.fail("上传失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询文件列表。
     */
    @SaCheckPermission("system:file:list")
    @GetMapping("/page")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysFile>> page(com.nz.admin.modules.system.query.SysFileQuery query) {
        return R.ok(fileService.listPage(query));
    }

    /**
     * 按 id 删掉文件记录。
     */
    @SaCheckPermission("system:file:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        SysFile sysFile = fileService.getById(id);
        if (sysFile != null && FileUtil.exist(sysFile.getFilePath())) {
            FileUtil.del(sysFile.getFilePath());
        }
        fileService.removeById(id);
        return R.ok();
    }

    /**
     * 下载文件。
     */
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        File file = new File(basePath + "/" + fileName);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLength((int) file.length());
        FileUtil.writeToStream(file, response.getOutputStream());
    }

    private SysFile saveFile(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String fileExt = FileUtil.extName(originalName);
        String fileName = IdUtil.fastSimpleUUID() + (StrUtil.isNotBlank(fileExt) ? "." + fileExt : "");
        String filePath = basePath + "/" + fileName;

        File dest = new File(filePath);
        FileUtil.mkParentDirs(dest);
        file.transferTo(dest);

        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(originalName);
        sysFile.setFileName(fileName);
        sysFile.setFilePath(filePath);
        sysFile.setFileUrl("/api/system/file/download/" + fileName);
        sysFile.setFileSize(file.getSize());
        sysFile.setFileExt(fileExt);
        sysFile.setMimeType(file.getContentType());
        sysFile.setUploaderId(cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong());
        return sysFile;
    }
}
