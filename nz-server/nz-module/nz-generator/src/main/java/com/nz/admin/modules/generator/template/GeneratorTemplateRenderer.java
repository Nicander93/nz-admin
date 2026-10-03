package com.nz.admin.modules.generator.template;

import com.nz.admin.common.core.BusinessException;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 根据外置模板生成 nz-admin 分层代码。
 */
@Component
public class GeneratorTemplateRenderer {

    private static final List<TemplateDefinition> TEMPLATES = List.of(
            new TemplateDefinition("backend/do.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/entity/dataobject/@@CLASS@@DO.java"),
            new TemplateDefinition("backend/query.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/entity/query/@@CLASS@@Query.java"),
            new TemplateDefinition("backend/create-request.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/entity/dto/@@CLASS@@CreateRequest.java"),
            new TemplateDefinition("backend/update-request.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/entity/dto/@@CLASS@@UpdateRequest.java"),
            new TemplateDefinition("backend/vo.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/entity/vo/@@CLASS@@VO.java"),
            new TemplateDefinition("backend/convert.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/convert/@@CLASS@@Convert.java"),
            new TemplateDefinition("backend/mapper.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/mapper/@@CLASS@@Mapper.java"),
            new TemplateDefinition("backend/service.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/service/@@CLASS@@Service.java"),
            new TemplateDefinition("backend/service-impl.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/service/@@CLASS@@ServiceImpl.java"),
            new TemplateDefinition("backend/controller.java.tpl",
                    "nz-server/nz-module/nz-@@MODULE@@/src/main/java/@@PACKAGE_PATH@@/controller/@@CLASS@@Controller.java"),
            new TemplateDefinition("frontend/api.ts.tpl", "nz-web/src/api/@@MODULE@@/@@BUSINESS@@.ts"),
            new TemplateDefinition("frontend/hooks.ts.tpl", "nz-web/src/views/@@MODULE@@/@@BUSINESS@@/hooks.ts"),
            new TemplateDefinition("frontend/index.vue.tpl", "nz-web/src/views/@@MODULE@@/@@BUSINESS@@/index.vue"),
            new TemplateDefinition("sql/menu.sql.tpl", "sql/@@MODULE@@_@@BUSINESS@@_menu.sql")
    );

    public Map<String, String> render(GeneratorRequest request, List<GeneratorColumn> columns) {
        List<ColumnModel> models = columns.stream()
                .sorted(Comparator.comparing(GeneratorColumn::getOrdinalPosition))
                .map(this::toModel)
                .toList();
        ColumnModel primaryKey = models.stream().filter(ColumnModel::primaryKey).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Primary key is required"));

        Map<String, String> context = buildContext(request, models, primaryKey);
        Map<String, String> files = new LinkedHashMap<>();
        for (TemplateDefinition definition : TEMPLATES) {
            files.put(replaceTokens(definition.outputPath(), context),
                    replaceTokens(readTemplate(definition.resourcePath()), context));
        }
        files.put("INSTALL-" + request.getModuleName() + "-" + request.getBusinessName() + ".md",
                "# 生成业务安装\n\n目标业务模块需依赖 nz-starter-tenant、nz-starter-datascope、nz-starter-auth。\n"
                + "配置类放在模块扫描包下，自动注册租户与数据权限。归属字段由服务端写入，更新不会变更归属。\n"
                + "菜单 SQL 应通过新的 Flyway 迁移交付；已发布版本不得修改。\n"
                + "验收必须覆盖跨租户不可见、越权详情/更新/删除，以及客户端伪造归属字段。\n");
        String tenant = ownershipColumn(request.getTenantColumn(), "tenant_id", models);
        if (tenant != null && !"tenant_id".equals(tenant))
            throw new BusinessException("租户列必须使用框架约定 tenant_id");
        String dept = ownershipColumn(request.getDeptColumn(), "dept_id", models);
        String owner = ownershipColumn(request.getOwnerColumn(), "owner_id", models);
        if (tenant != null || request.isDataScopeEnabled() && (dept != null || owner != null)) {
            String beans = tenant == null ? "" : "    @Bean\n    public com.nz.admin.framework.tenant.core.TenantTableRuleCustomizer tenantTables() {\n"
                    + "        return tables -> tables.add(\"" + request.getTableName() + "\");\n    }\n";
            if (request.isDataScopeEnabled() && (dept != null || owner != null)) {
                beans += "    @Bean\n    public com.nz.admin.framework.datascope.core.DataScopeRuleCustomizer dataScopeRules() {\n"
                        + "        return rules -> rules.add(new com.nz.admin.framework.datascope.core.DataScopeRule(\""
                        + request.getTableName() + "\", " + quoted(dept) + ", " + quoted(owner) + "));\n    }\n";
            }
            files.put("nz-server/nz-module/nz-" + request.getModuleName() + "/src/main/java/"
                    + request.getPackageName().replace('.', '/') + "/config/" + request.getClassName() + "AccessConfiguration.java",
                    "package " + request.getPackageName() + ".config;\n\nimport org.springframework.context.annotation.Bean;\n"
                    + "import org.springframework.context.annotation.Configuration;\n\n@Configuration(proxyBeanMethods = false)\n"
                    + "public class " + request.getClassName() + "AccessConfiguration {\n" + beans + "}\n");
        }
        return files;
    }

    private String quoted(String value) { return value == null ? "null" : "\"" + value + "\""; }

    private String ownershipColumn(String configured, String standard, List<ColumnModel> columns) {
        String name = StrUtil.isBlank(configured) ? standard : configured;
        var found = columns.stream().filter(column -> column.columnName().equals(name)).findFirst();
        if (found.isEmpty()) {
            if (StrUtil.isNotBlank(configured)) throw new IllegalArgumentException("归属字段不存在：" + name);
            return null;
        }
        if (!"Long".equals(found.get().javaType()) || found.get().primaryKey())
            throw new IllegalArgumentException("归属字段必须为非主键 BIGINT：" + name);
        return name;
    }

    private Map<String, String> buildContext(GeneratorRequest request, List<ColumnModel> columns,
                                             ColumnModel primaryKey) {
        List<ColumnModel> entityColumns = columns.stream().filter(column -> !column.audit()).toList();
        var protectedColumns = new LinkedHashSet<>(Set.of("tenant_id", "owner_id", "dept_id", "create_by", "update_by"));
        String tenant = ownershipColumn(request.getTenantColumn(), "tenant_id", columns);
        String dept = ownershipColumn(request.getDeptColumn(), "dept_id", columns);
        String owner = ownershipColumn(request.getOwnerColumn(), "owner_id", columns);
        for (String field : new String[]{tenant, dept, owner}) if (field != null) protectedColumns.add(field);
        List<ColumnModel> writableColumns = entityColumns.stream()
                .filter(column -> !column.primaryKey() && !protectedColumns.contains(column.columnName())).toList();
        List<ColumnModel> queryColumns = writableColumns.stream().filter(column -> !column.binary()).toList();

        Map<String, String> context = new LinkedHashMap<>();
        context.put("PACKAGE", request.getPackageName());
        context.put("PACKAGE_PATH", request.getPackageName().replace('.', '/'));
        context.put("CLASS", request.getClassName());
        context.put("CLASS_CAMEL", lowerFirst(request.getClassName()));
        context.put("TABLE", request.getTableName());
        context.put("SCHEMA", request.getSchemaName());
        context.put("OWNERSHIP_FIELDS", tenant == null && dept == null && owner == null ? "" :
                "    @org.springframework.beans.factory.annotation.Autowired\n"
                + "    private com.nz.admin.framework.auth.core.LoginUserContext loginUserContext;\n");
        String assignment = "";
        if (tenant != null) assignment += "        Long tenant = com.nz.admin.framework.tenant.core.TenantContextHolder.getTenantIdOrNull();\n"
                + "        if (tenant == null) throw new BusinessException(\"缺少可信租户身份\");\n"
                + "        entity.set" + upperFirst(toCamelCase(tenant)) + "(tenant);\n";
        if (dept != null || owner != null) {
            assignment += "        var identity = loginUserContext.getLoginUserOrNull();\n"
                    + "        if (identity == null) throw new BusinessException(\"需要登录身份\");\n";
            if (owner != null) assignment += "        entity.set" + upperFirst(toCamelCase(owner)) + "(identity.getUserId());\n";
            if (dept != null) assignment += "        if (identity.getDeptId() == null) throw new BusinessException(\"当前用户未关联部门\");\n"
                    + "        entity.set" + upperFirst(toCamelCase(dept)) + "(identity.getDeptId());\n";
        }
        context.put("OWNERSHIP_ASSIGNMENT", assignment);
        context.put("MODULE", request.getModuleName());
        context.put("BUSINESS", request.getBusinessName());
        context.put("FEATURE_DOC", javadoc(request.getFeatureName()));
        context.put("FEATURE_JAVA", javaString(request.getFeatureName()));
        context.put("FEATURE_TS", typeScriptString(request.getFeatureName()));
        context.put("FEATURE_HTML", vueLabel(request.getFeatureName()));
        context.put("AUTHOR", javadoc(request.getAuthor()));
        context.put("PARENT_MENU_ID", String.valueOf(request.getParentMenuId()));
        context.put("PERMISSION_PREFIX", request.getModuleName() + ":" + request.getBusinessName());
        context.put("PK_TYPE", primaryKey.javaType());
        context.put("TS_PK_TYPE", primaryKey.tsType());
        context.put("PK_FIELD", primaryKey.fieldName());
        context.put("PK_GETTER", upperFirst(primaryKey.fieldName()));
        context.put("ENTITY_IMPORTS", javaImports(entityColumns));
        context.put("ENTITY_FIELDS", entityFields(entityColumns));
        context.put("QUERY_IMPORTS", javaImports(queryColumns));
        context.put("QUERY_FIELDS", plainFields(queryColumns, false));
        context.put("CREATE_IMPORTS", javaImports(writableColumns));
        context.put("CREATE_FIELDS", createFields(writableColumns));
        context.put("VO_IMPORTS", javaImports(columns));
        context.put("VO_FIELDS", plainFields(columns, true));
        context.put("QUERY_CONDITIONS", queryConditions(queryColumns, request.getClassName()));
        context.put("TS_FIELDS", tsFields(columns));
        context.put("TS_QUERY_FIELDS", tsQueryFields(queryColumns));
        context.put("TS_DEFAULT_FORM", tsDefaultForm(writableColumns));
        context.put("VUE_QUERY_FIELDS", vueQueryFields(queryColumns.stream().limit(3).toList()));
        context.put("VUE_COLUMNS", vueColumns(entityColumns));
        context.put("VUE_FORM_FIELDS", vueFormFields(writableColumns));
        context.put("SQL_FEATURE", sqlLiteral(request.getFeatureName()));
        return context;
    }

    private ColumnModel toModel(GeneratorColumn column) {
        String javaType = javaType(column);
        return new ColumnModel(
                column.getColumnName(),
                toCamelCase(column.getColumnName()),
                javaType,
                tsType(javaType),
                javadoc(StrUtil.blankToDefault(column.getColumnComment(), column.getColumnName())),
                Boolean.TRUE.equals(column.getNullable()),
                Boolean.TRUE.equals(column.getPrimaryKey()),
                Boolean.TRUE.equals(column.getIdentity())
                        || StrUtil.containsIgnoreCase(column.getDefaultValue(), "nextval"),
                isAuditColumn(column.getColumnName()),
                "byte[]".equals(javaType),
                column.getDefaultValue()
        );
    }

    private String javaType(GeneratorColumn column) {
        String dataType = StrUtil.blankToDefault(column.getDataType(), "").toLowerCase(Locale.ROOT);
        String udtName = StrUtil.blankToDefault(column.getUdtName(), "").toLowerCase(Locale.ROOT);
        return switch (udtName) {
            case "int8" -> Long.class.getSimpleName();
            case "int2", "int4" -> Integer.class.getSimpleName();
            case "float4" -> Float.class.getSimpleName();
            case "float8" -> Double.class.getSimpleName();
            case "numeric", "decimal" -> BigDecimal.class.getSimpleName();
            case "bool" -> Boolean.class.getSimpleName();
            case "date" -> LocalDate.class.getSimpleName();
            case "time", "timetz" -> LocalTime.class.getSimpleName();
            case "timestamp", "timestamptz" -> LocalDateTime.class.getSimpleName();
            case "uuid" -> UUID.class.getSimpleName();
            case "bytea" -> "byte[]";
            default -> switch (dataType) {
                case "bigint" -> Long.class.getSimpleName();
                case "smallint", "integer" -> Integer.class.getSimpleName();
                case "real" -> Float.class.getSimpleName();
                case "double precision" -> Double.class.getSimpleName();
                case "numeric", "decimal" -> BigDecimal.class.getSimpleName();
                case "boolean" -> Boolean.class.getSimpleName();
                case "date" -> LocalDate.class.getSimpleName();
                case "time without time zone", "time with time zone" -> LocalTime.class.getSimpleName();
                case "timestamp without time zone", "timestamp with time zone" -> LocalDateTime.class.getSimpleName();
                case "uuid" -> UUID.class.getSimpleName();
                case "bytea" -> "byte[]";
                default -> String.class.getSimpleName();
            };
        };
    }

    private String javaImports(List<ColumnModel> columns) {
        Set<String> imports = new LinkedHashSet<>();
        for (ColumnModel column : columns) {
            switch (column.javaType()) {
                case "BigDecimal" -> imports.add("java.math.BigDecimal");
                case "LocalDate" -> imports.add("java.time.LocalDate");
                case "LocalDateTime" -> imports.add("java.time.LocalDateTime");
                case "LocalTime" -> imports.add("java.time.LocalTime");
                case "UUID" -> imports.add("java.util.UUID");
                default -> {
                }
            }
        }
        return imports.stream().sorted().map(value -> "import " + value + ";")
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String entityFields(List<ColumnModel> columns) {
        List<String> blocks = new ArrayList<>();
        for (ColumnModel column : columns) {
            StringBuilder block = new StringBuilder("    /** ").append(column.comment()).append(" */\n");
            if (column.primaryKey()) {
                block.append("    @TableId(type = IdType.")
                        .append(column.autoIncrement() ? "AUTO" : "INPUT")
                        .append(")\n");
            }
            block.append("    private ").append(column.javaType()).append(" ").append(column.fieldName()).append(";");
            blocks.add(block.toString());
        }
        return String.join("\n\n", blocks);
    }

    private String plainFields(List<ColumnModel> columns, boolean includeAudit) {
        List<String> blocks = new ArrayList<>();
        for (ColumnModel column : columns) {
            if (!includeAudit && column.audit()) {
                continue;
            }
            blocks.add("    /** " + column.comment() + " */\n"
                    + "    private " + column.javaType() + " " + column.fieldName() + ";");
        }
        return String.join("\n\n", blocks);
    }

    private String createFields(List<ColumnModel> columns) {
        List<String> blocks = new ArrayList<>();
        for (ColumnModel column : columns) {
            StringBuilder block = new StringBuilder("    /** ").append(column.comment()).append(" */\n");
            if (!column.nullable() && StrUtil.isBlank(column.defaultValue())) {
                block.append("    ").append("String".equals(column.javaType()) ? "@NotBlank" : "@NotNull").append("\n");
            }
            block.append("    private ").append(column.javaType()).append(" ").append(column.fieldName()).append(";");
            blocks.add(block.toString());
        }
        return String.join("\n\n", blocks);
    }

    private String queryConditions(List<ColumnModel> columns, String className) {
        List<String> conditions = new ArrayList<>();
        for (ColumnModel column : columns) {
            String getter = upperFirst(column.fieldName());
            String condition = "String".equals(column.javaType())
                    ? ".like(StrUtil.isNotBlank(query.get" + getter + "()), " + className + "DO::get" + getter
                    + ", query.get" + getter + "())"
                    : ".eq(query.get" + getter + "() != null, " + className + "DO::get" + getter
                    + ", query.get" + getter + "())";
            conditions.add("                " + condition);
        }
        return String.join("\n", conditions);
    }

    private String tsFields(List<ColumnModel> columns) {
        return columns.stream()
                .map(column -> "  " + column.fieldName() + (column.nullable() ? "?" : "") + ": " + column.tsType())
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String tsQueryFields(List<ColumnModel> columns) {
        return columns.stream().map(column -> "  " + column.fieldName() + "?: " + column.tsType())
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String tsDefaultForm(List<ColumnModel> columns) {
        return columns.stream().map(column -> "      " + column.fieldName() + ": "
                        + tsDefaultValue(column.javaType()) + ",")
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String vueQueryFields(List<ColumnModel> columns) {
        List<String> blocks = new ArrayList<>();
        for (ColumnModel column : columns) {
            String label = vueLabel(column.comment());
            String component = isNumberType(column.javaType())
                    ? "<el-input-number v-model=\"table.query." + column.fieldName()
                    + "\" controls-position=\"right\" />"
                    : "<el-input v-model=\"table.query." + column.fieldName()
                    + "\" clearable placeholder=\"请输入" + label + "\" />";
            blocks.add("      <el-form-item label=\"" + label + "\">\n        " + component
                    + "\n      </el-form-item>");
        }
        return String.join("\n", blocks);
    }

    private String vueColumns(List<ColumnModel> columns) {
        return columns.stream()
                .map(column -> "      <el-table-column prop=\"" + column.fieldName() + "\" label=\""
                        + vueLabel(column.comment()) + "\" min-width=\"130\" show-overflow-tooltip />")
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String vueFormFields(List<ColumnModel> columns) {
        List<String> blocks = new ArrayList<>();
        for (ColumnModel column : columns) {
            String label = vueLabel(column.comment());
            String component;
            if ("Boolean".equals(column.javaType())) {
                component = "<el-switch v-model=\"form.model." + column.fieldName() + "\" />";
            } else if (isNumberType(column.javaType())) {
                component = "<el-input-number v-model=\"form.model." + column.fieldName()
                        + "\" style=\"width: 100%\" />";
            } else if ("LocalDate".equals(column.javaType()) || "LocalDateTime".equals(column.javaType())) {
                component = "<el-date-picker v-model=\"form.model." + column.fieldName()
                        + "\" type=\"datetime\" value-format=\"YYYY-MM-DDTHH:mm:ss\" style=\"width: 100%\" />";
            } else {
                component = "<el-input v-model=\"form.model." + column.fieldName() + "\" />";
            }
            blocks.add("        <el-form-item label=\"" + label + "\">\n          " + component
                    + "\n        </el-form-item>");
        }
        return String.join("\n", blocks);
    }

    private String replaceTokens(String source, Map<String, String> context) {
        String result = source;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            result = result.replace("@@" + entry.getKey() + "@@", entry.getValue());
        }
        return result;
    }

    private String readTemplate(String resourcePath) {
        String fullPath = "generator/templates/" + resourcePath;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fullPath)) {
            if (input == null) {
                throw new IllegalStateException("生成模板不存在: " + fullPath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取生成模板失败: " + fullPath, exception);
        }
    }

    private boolean isAuditColumn(String columnName) {
        return "create_time".equalsIgnoreCase(columnName) || "update_time".equalsIgnoreCase(columnName);
    }

    private boolean isNumberType(String javaType) {
        return Set.of("Long", "Integer", "Float", "Double", "BigDecimal").contains(javaType);
    }

    private String tsType(String javaType) {
        if (isNumberType(javaType)) {
            return "number";
        }
        if ("Boolean".equals(javaType)) {
            return "boolean";
        }
        return "string";
    }

    private String tsDefaultValue(String javaType) {
        if (isNumberType(javaType)) {
            return "0";
        }
        if ("Boolean".equals(javaType)) {
            return "false";
        }
        return "''";
    }

    private String toCamelCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char current : value.toCharArray()) {
            if (current == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(current));
                upperNext = false;
            } else {
                result.append(Character.toLowerCase(current));
            }
        }
        return result.toString();
    }

    private String lowerFirst(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private String upperFirst(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String javadoc(String value) {
        return StrUtil.blankToDefault(value, "").replace("*/", "* /").replaceAll("[\\r\\n]+", " ").trim();
    }

    private String javaString(String value) {
        return javadoc(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String typeScriptString(String value) {
        return javadoc(value).replace("\\", "\\\\").replace("'", "\\'");
    }

    private String vueLabel(String value) {
        return javadoc(value).replace("\"", "＂").replace("<", "＜").replace(">", "＞");
    }

    private String sqlLiteral(String value) {
        return StrUtil.blankToDefault(value, "").replace("'", "''");
    }

    private record TemplateDefinition(String resourcePath, String outputPath) {
    }

    private record ColumnModel(String columnName, String fieldName, String javaType, String tsType, String comment,
                               boolean nullable, boolean primaryKey, boolean autoIncrement, boolean audit,
                               boolean binary, String defaultValue) {
    }
}
