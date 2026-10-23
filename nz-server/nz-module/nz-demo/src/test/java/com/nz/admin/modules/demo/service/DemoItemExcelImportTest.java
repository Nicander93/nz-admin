package com.nz.admin.modules.demo.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.modules.demo.entity.excel.DemoItemExcelRow;
import com.nz.admin.modules.demo.mapper.DemoItemMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DemoItemExcelImportTest {

    @Test
    void parsesWorkbookAndReportsInvalidRowsWithoutPersisting() throws Exception {
        var output = new ByteArrayOutputStream();
        DemoItemExcelRow valid = row("有效条目", 0);
        DemoItemExcelRow invalid = row("", 7);
        EasyExcel.write(output, DemoItemExcelRow.class).sheet().doWrite(List.of(valid, invalid));
        var service = service();

        var result = service.previewImport(new MockMultipartFile("file", "items.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()));

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(error -> assertThat(error.getRowNum()).isEqualTo(3));
    }

    @Test
    void reportsPhysicalExcelRowAfterBlankRows() throws Exception {
        var original = new ByteArrayOutputStream();
        EasyExcel.write(original, DemoItemExcelRow.class).sheet().doWrite(List.of(row("valid", 0), row("", 7)));
        var output = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(original.toByteArray()))) {
            workbook.getSheetAt(0).shiftRows(2, 2, 1);
            workbook.write(output);
        }
        var result = service().previewImport(new MockMultipartFile("file", "blank-row.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getErrors()).isNotEmpty()
                .allSatisfy(error -> assertThat(error.getRowNum()).isEqualTo(4));
    }

    @Test
    void reportsInvalidNumericCellAndContinuesPreviewingLaterRows() throws Exception {
        var original = new ByteArrayOutputStream();
        EasyExcel.write(original, DemoItemExcelRow.class).sheet().doWrite(List.of(row("bad number", 0), row("valid", 0)));
        var output = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(original.toByteArray()))) {
            workbook.getSheetAt(0).getRow(1).getCell(2).setCellValue("not a number");
            workbook.write(output);
        }
        var result = service().previewImport(new MockMultipartFile("file", "invalid-cell.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()));
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getRows()).extracting("name").containsExactly("valid");
        assertThat(result.getErrors()).singleElement().satisfies(error -> {
            assertThat(error.getRowNum()).isEqualTo(2);
            assertThat(error.getMessage()).contains("第 3 列", "数据格式不正确");
        });
    }

    @Test
    void exportsUserTextAsTextWhenItStartsWithFormulaCharacters() {
        var service = service();
        assertThat(service.excelText("=SUM(A1:A2)")).isEqualTo("'=SUM(A1:A2)");
        assertThat(service.excelText("+cmd|' /C calc'!A0")).startsWith("'");
        assertThat(service.excelText("ordinary text")).isEqualTo("ordinary text");
    }

    @Test
    void rejectsMisalignedHeaders() throws Exception {
        var output = new ByteArrayOutputStream();
        EasyExcel.write(output, WrongHeaderRow.class).sheet().doWrite(List.of(new WrongHeaderRow("misaligned")));
        assertThatThrownBy(() -> service().previewImport(new MockMultipartFile("file", "misaligned.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray())))
                .hasMessageContaining("表头");
    }

    @Test
    void rejectsInvalidWorkbookAndTooManyRows() throws Exception {
        var service = service();
        assertThatThrownBy(() -> service.previewImport(new MockMultipartFile("file", "bad.xlsx", "application/octet-stream", new byte[]{1, 2, 3})))
                .isInstanceOf(RuntimeException.class);
        var output = new ByteArrayOutputStream();
        EasyExcel.write(output, DemoItemExcelRow.class).sheet().doWrite(java.util.Collections.nCopies(1001, row("too many", 0)));
        assertThatThrownBy(() -> service.previewImport(new MockMultipartFile("file", "large.xlsx", "application/octet-stream", output.toByteArray())))
                .hasMessageContaining("1000");
        assertThatThrownBy(() -> service.previewImport(new MockMultipartFile("file", "oversize.xlsx", "application/octet-stream", new byte[10 * 1024 * 1024 + 1])))
                .hasMessageContaining("10MB");
    }

    private DemoItemServiceImpl service() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var service = new DemoItemServiceImpl(mock(LoginUserContext.class), validator);
        ReflectionTestUtils.setField(service, "baseMapper", mock(DemoItemMapper.class));
        return service;
    }

    private static class WrongHeaderRow {
        @ExcelProperty(value = "名称", index = 0)
        private final String name;

        WrongHeaderRow(String name) { this.name = name; }
    }

    private DemoItemExcelRow row(String name, int status) {
        var row = new DemoItemExcelRow();
        row.setName(name);
        row.setCategory("import-test");
        row.setStatus(status);
        row.setSort(1);
        row.setRemark("test");
        return row;
    }
}
