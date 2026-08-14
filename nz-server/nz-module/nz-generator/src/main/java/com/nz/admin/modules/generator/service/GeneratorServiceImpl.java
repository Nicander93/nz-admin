package com.nz.admin.modules.generator.service;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorPreview;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import com.nz.admin.modules.generator.model.GeneratorTable;
import com.nz.admin.modules.generator.repository.GeneratorMetadataRepository;
import com.nz.admin.modules.generator.template.GeneratorTemplateRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成服务实现。
 */
@Service
public class GeneratorServiceImpl implements GeneratorService {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern MODULE_NAME = Pattern.compile("[a-z][A-Za-z0-9]*");
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern PACKAGE_NAME =
            Pattern.compile("[a-z_][A-Za-z0-9_]*(\\.[a-z_][A-Za-z0-9_]*)+");

    private final GeneratorMetadataRepository metadataRepository;
    private final GeneratorTemplateRenderer templateRenderer;

    public GeneratorServiceImpl(GeneratorMetadataRepository metadataRepository,
                                GeneratorTemplateRenderer templateRenderer) {
        this.metadataRepository = metadataRepository;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public List<GeneratorTable> listTables(String schemaName, String keyword) {
        validateIdentifier(schemaName, "Schema");
        return metadataRepository.listTables(schemaName, StrUtil.blankToDefault(keyword, ""));
    }

    @Override
    public List<GeneratorColumn> listColumns(String schemaName, String tableName) {
        validateIdentifier(schemaName, "Schema");
        validateIdentifier(tableName, "表名");
        List<GeneratorColumn> columns = metadataRepository.listColumns(schemaName, tableName);
        if (columns.isEmpty()) {
            throw new BusinessException("数据表不存在或没有可读取的字段");
        }
        return columns;
    }

    @Override
    public GeneratorPreview preview(GeneratorRequest request) {
        validateRequest(request);
        List<GeneratorColumn> columns = listColumns(request.getSchemaName(), request.getTableName());
        long primaryKeyCount = columns.stream().filter(column -> Boolean.TRUE.equals(column.getPrimaryKey())).count();
        if (primaryKeyCount != 1) {
            throw new BusinessException("代码生成仅支持单主键表，当前主键列数量为 " + primaryKeyCount);
        }

        GeneratorPreview preview = new GeneratorPreview();
        preview.setColumns(columns);
        preview.setFiles(templateRenderer.render(request, columns));
        return preview;
    }

    @Override
    public byte[] download(GeneratorRequest request) {
        Map<String, String> files = preview(request).getFiles();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                validateZipPath(file.getKey());
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("生成 ZIP 文件失败");
        }
    }

    private void validateRequest(GeneratorRequest request) {
        if (request == null
                || !MODULE_NAME.matcher(StrUtil.blankToDefault(request.getModuleName(), "")).matches()
                || !MODULE_NAME.matcher(StrUtil.blankToDefault(request.getBusinessName(), "")).matches()
                || !CLASS_NAME.matcher(StrUtil.blankToDefault(request.getClassName(), "")).matches()
                || !PACKAGE_NAME.matcher(StrUtil.blankToDefault(request.getPackageName(), "")).matches()) {
            throw new BusinessException("代码生成参数格式不正确");
        }
        if (StrUtil.isBlank(request.getFeatureName()) || StrUtil.isBlank(request.getAuthor())
                || request.getParentMenuId() == null || request.getParentMenuId() < 0) {
            throw new BusinessException("代码生成参数不完整");
        }
    }

    private void validateIdentifier(String value, String fieldName) {
        if (StrUtil.isBlank(value) || !IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException(fieldName + "格式不正确");
        }
    }

    private void validateZipPath(String path) {
        if (StrUtil.isBlank(path) || path.startsWith("/") || path.contains("..") || path.contains("\\")) {
            throw new BusinessException("生成文件路径不安全");
        }
    }
}
