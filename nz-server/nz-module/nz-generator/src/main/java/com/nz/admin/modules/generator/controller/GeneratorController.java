package com.nz.admin.modules.generator.controller;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.modules.generator.model.GeneratorColumn;
import com.nz.admin.modules.generator.model.GeneratorPreview;
import com.nz.admin.modules.generator.model.GeneratorRequest;
import com.nz.admin.modules.generator.model.GeneratorTable;
import com.nz.admin.modules.generator.service.GeneratorService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 代码生成接口。
 */
@RestController
@RequestMapping("/api/generator")
public class GeneratorController {

    private final GeneratorService generatorService;

    public GeneratorController(GeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @SaCheckPermission("generator:table:list")
    @GetMapping("/tables")
    public R<List<GeneratorTable>> listTables(
            @RequestParam(defaultValue = "public") String schemaName,
            @RequestParam(required = false) String keyword) {
        return R.ok(generatorService.listTables(schemaName, keyword));
    }

    @SaCheckPermission("generator:table:query")
    @GetMapping("/columns")
    public R<List<GeneratorColumn>> listColumns(
            @RequestParam(defaultValue = "public") String schemaName,
            @RequestParam String tableName) {
        return R.ok(generatorService.listColumns(schemaName, tableName));
    }

    @SaCheckPermission("generator:table:preview")
    @PostMapping("/preview")
    public R<GeneratorPreview> preview(@Valid @RequestBody GeneratorRequest request) {
        return R.ok(generatorService.preview(request));
    }

    @SaCheckPermission("generator:table:download")
    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@Valid @RequestBody GeneratorRequest request) {
        String filename = request.getModuleName() + "-" + request.getBusinessName() + ".zip";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(generatorService.download(request));
    }
}
