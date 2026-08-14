package com.nz.admin.modules.generator.service;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorPreview;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import com.nz.admin.modules.generator.repository.GeneratorMetadataRepository;
import com.nz.admin.modules.generator.template.GeneratorTemplateRenderer;
import com.nz.admin.modules.generator.template.GeneratorTemplateRendererTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 代码生成服务测试。
 */
class GeneratorServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private GeneratorMetadataRepository metadataRepository;

    private GeneratorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GeneratorServiceImpl(metadataRepository, new GeneratorTemplateRenderer());
    }

    @Test
    void previewUsesDatabaseColumnsAndExternalTemplates() {
        GeneratorRequest request = GeneratorTemplateRendererTest.request();
        when(metadataRepository.listColumns("public", "demo_item"))
                .thenReturn(GeneratorTemplateRendererTest.columns());

        GeneratorPreview preview = service.preview(request);

        assertThat(preview.getColumns()).hasSize(6);
        assertThat(preview.getFiles()).hasSize(14);
        assertThat(preview.getFiles().keySet()).anyMatch(path -> path.endsWith("DemoItemController.java"));
    }

    @Test
    void rejectsTablesWithoutExactlyOnePrimaryKey() {
        GeneratorRequest request = GeneratorTemplateRendererTest.request();
        List<GeneratorColumn> columns = new ArrayList<>(GeneratorTemplateRendererTest.columns());
        columns.get(0).setPrimaryKey(false);
        when(metadataRepository.listColumns("public", "demo_item")).thenReturn(columns);

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持单主键表");
    }

    @Test
    void rejectsUnsafeOutputNamesBeforeReadingMetadata() {
        GeneratorRequest request = GeneratorTemplateRendererTest.request();
        request.setModuleName("../demo");

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数格式不正确");
    }

    @Test
    void downloadContainsTheSameGeneratedFilesAsPreview() throws Exception {
        GeneratorRequest request = GeneratorTemplateRendererTest.request();
        when(metadataRepository.listColumns("public", "demo_item"))
                .thenReturn(GeneratorTemplateRendererTest.columns());

        byte[] archive = service.download(request);
        List<String> entries = new ArrayList<>();
        String controller = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(entry.getName());
                if (entry.getName().endsWith("DemoItemController.java")) {
                    controller = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        assertThat(entries).hasSize(14);
        assertThat(entries).contains("sql/demo_item_menu.sql");
        assertThat(controller).contains("@RequestMapping(\"/api/demo/item\")");
    }
}
