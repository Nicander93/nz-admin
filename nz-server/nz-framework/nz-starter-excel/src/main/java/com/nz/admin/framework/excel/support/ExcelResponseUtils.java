package com.nz.admin.framework.excel.support;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 响应工具。
 */
public final class ExcelResponseUtils {

    private ExcelResponseUtils() {
    }

    public static <T> void write(HttpServletResponse response, String fileName, String sheetName, Class<T> headClass, List<T> data)
            throws IOException {
        prepareResponse(response, fileName);
        EasyExcel.write(response.getOutputStream(), headClass)
                .sheet(sheetName)
                .doWrite(data);
    }

    public static void prepareResponse(HttpServletResponse response, String fileName) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
    }
}
