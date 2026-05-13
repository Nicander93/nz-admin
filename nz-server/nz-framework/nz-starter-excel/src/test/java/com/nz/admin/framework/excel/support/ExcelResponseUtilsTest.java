package com.nz.admin.framework.excel.support;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelResponseUtilsTest {

    @Test
    void shouldPrepareExcelDownloadHeaders() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();

        ExcelResponseUtils.prepareResponse(response, "login-log");

        assertThat(response.getContentType()).contains("spreadsheetml");
        assertThat(response.getHeader("Content-Disposition")).contains("login-log.xlsx");
    }
}
