package com.nz.admin.modules.generator.template;

import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 代码模板渲染测试。
 */
public class GeneratorTemplateRendererTest {

    private final GeneratorTemplateRenderer renderer = new GeneratorTemplateRenderer();

    @Test
    void rendersCompleteNzAdminCrudSliceWithoutUnresolvedTokens() {
        Map<String, String> files = renderer.render(request(), columns());

        assertThat(files).hasSize(14);
        assertThat(files).containsKeys(
                "nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/entity/dataobject/DemoItemDO.java",
                "nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/controller/DemoItemController.java",
                "nz-web/src/api/demo/item.ts",
                "nz-web/src/views/demo/item/index.vue",
                "sql/demo_item_menu.sql"
        );
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("@@"));
        assertThat(files.values()).anySatisfy(content -> assertThat(content)
                .contains("@TableName(\"demo_item\")", "private Long id;", "private String itemName;"));
        assertThat(files.values()).anySatisfy(content -> assertThat(content)
                .contains("entity.getId()", "request.getId()", "DemoItemDO::getId"));
        assertThat(files.get("sql/demo_item_menu.sql"))
                .contains("demo:item:list", "demo:item:add", "ON CONFLICT DO NOTHING");
    }

    @Test
    void generatedBackendSourcesCompile(@TempDir Path tempDirectory) throws IOException {
        Map<String, String> files = renderer.render(request(), columns());
        String fixtureOutput = System.getProperty("nz.generator.fixture.output");
        if (fixtureOutput != null && !fixtureOutput.isBlank()) {
            Path fixtureRoot = Path.of(fixtureOutput);
            files.forEach((path, content) -> writeSource(fixtureRoot, path, content));
        }
        List<Path> sourcePaths = files.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(".java"))
                .map(entry -> writeSource(tempDirectory, entry.getKey(), entry.getValue()))
                .toList();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path output = Files.createDirectories(tempDirectory.resolve("classes"));
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            var units = fileManager.getJavaFileObjectsFromPaths(sourcePaths);
            boolean compiled = compiler.getTask(null, fileManager, diagnostics,
                    List.of("-classpath", System.getProperty("java.class.path"), "-d", output.toString()),
                    null, units).call();
            assertThat(compiled)
                    .withFailMessage(() -> diagnostics.getDiagnostics().toString())
                    .isTrue();
        }
    }

    private static Path writeSource(Path root, String relativePath, String content) {
        try {
            Path target = root.resolve(relativePath);
            Files.createDirectories(target.getParent());
            return Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void escapesFeatureNameForEachGeneratedLanguage() {
        GeneratorRequest request = request();
        request.setFeatureName("客户\"别名's");

        Map<String, String> files = renderer.render(request, columns());

        assertThat(files.get("nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/controller/DemoItemController.java"))
                .contains("@Log(title = \"客户\\\"别名's\",");
        assertThat(files.get("nz-web/src/views/demo/item/hooks.ts"))
                .contains("name: '客户\"别名\\'s'");
        assertThat(files.get("nz-web/src/views/demo/item/index.vue"))
                .contains("新增客户＂别名's", "确认删除该客户\"别名\\'s？");
        assertThat(files.get("sql/demo_item_menu.sql"))
                .contains("'客户\"别名''s'");
    }

    public static GeneratorRequest request() {
        GeneratorRequest request = new GeneratorRequest();
        request.setSchemaName("public");
        request.setTableName("demo_item");
        request.setModuleName("demo");
        request.setBusinessName("item");
        request.setClassName("DemoItem");
        request.setPackageName("com.nz.admin.modules.demo");
        request.setFeatureName("示例条目");
        request.setAuthor("tester");
        request.setParentMenuId(3000L);
        return request;
    }

    public static List<GeneratorColumn> columns() {
        return List.of(
                column(1, "id", "主键", "bigint", "int8", false, "nextval('demo_item_id_seq')", true, true),
                column(2, "item_name", "条目名称", "character varying", "varchar", false, null, false, false),
                column(3, "amount", "金额", "numeric", "numeric", true, null, false, false),
                column(4, "enabled", "是否启用", "boolean", "bool", false, "true", false, false),
                column(5, "create_time", "创建时间", "timestamp without time zone", "timestamp",
                        false, "CURRENT_TIMESTAMP", false, false),
                column(6, "update_time", "更新时间", "timestamp without time zone", "timestamp",
                        false, "CURRENT_TIMESTAMP", false, false)
        );
    }

    private static GeneratorColumn column(int position, String name, String comment, String dataType, String udtName,
                                          boolean nullable, String defaultValue, boolean primaryKey, boolean identity) {
        GeneratorColumn column = new GeneratorColumn();
        column.setOrdinalPosition(position);
        column.setColumnName(name);
        column.setColumnComment(comment);
        column.setDataType(dataType);
        column.setUdtName(udtName);
        column.setNullable(nullable);
        column.setDefaultValue(defaultValue);
        column.setPrimaryKey(primaryKey);
        column.setIdentity(identity);
        return column;
    }
}
