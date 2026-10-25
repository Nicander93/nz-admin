package com.nz.admin.framework.file;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 下载统一返回附件头，避免对象存储跨域重定向丢失应用鉴权。 */
final class FileDownloads {
    private FileDownloads() { }

    static void prepareResponse(HttpServletResponse response, FileStorageObject file) {
        String name = StrUtil.blankToDefault(file.getOriginalName(), file.getFileName());
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }
}
