package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowDefinitionUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowDefinitionVO;
import com.nz.admin.modules.workflow.service.WorkflowDefinitionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 流程定义接口。
 */
@RestController
@RequestMapping("/api/workflow/definition")
public class WorkflowDefinitionController {

    private static final long MAX_IMPORT_SIZE = 1024 * 1024;

    private final WorkflowDefinitionService definitionService;

    public WorkflowDefinitionController(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @SaCheckPermission("workflow:definition:list")
    @GetMapping("/page")
    public R<PageResult<WorkflowDefinitionVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String flowCode,
                                                     @RequestParam(required = false) String flowName,
                                                     @RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) Integer publishStatus) {
        return R.ok(definitionService.page(pageNum, pageSize, flowCode, flowName, categoryId, publishStatus));
    }

    @SaCheckPermission("workflow:definition:query")
    @GetMapping("/{definitionId}")
    public R<WorkflowDefinitionVO> get(@PathVariable Long definitionId) {
        return R.ok(definitionService.getRequired(definitionId));
    }

    @RepeatSubmit
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:definition:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody WorkflowDefinitionCreateRequest request) {
        return R.ok(definitionService.create(request));
    }

    @RepeatSubmit
    @Log(title = "流程定义", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:definition:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WorkflowDefinitionUpdateRequest request) {
        definitionService.update(request);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "流程定义发布", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:definition:publish")
    @PostMapping("/{definitionId}/publish")
    public R<Void> publish(@PathVariable Long definitionId) {
        definitionService.publish(definitionId);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "流程定义取消发布", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:definition:publish")
    @PostMapping("/{definitionId}/unpublish")
    public R<Void> unpublish(@PathVariable Long definitionId) {
        definitionService.unpublish(definitionId);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "流程定义运行状态", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:definition:active")
    @PutMapping("/{definitionId}/active")
    public R<Void> setActive(@PathVariable Long definitionId, @RequestParam boolean active) {
        definitionService.setActive(definitionId, active);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "复制流程定义", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:definition:copy")
    @PostMapping("/copy")
    public R<Long> copy(@Valid @RequestBody WorkflowDefinitionCopyRequest request) {
        return R.ok(definitionService.copy(request));
    }

    @Log(title = "导入流程定义", businessType = BusinessType.OTHER)
    @SaCheckPermission("workflow:definition:import")
    @PostMapping("/import")
    public R<Long> importJson(@RequestPart("file") MultipartFile file,
                              @RequestParam(required = false) Long categoryId) throws IOException {
        if (file.isEmpty() || file.getSize() > MAX_IMPORT_SIZE) {
            throw new BusinessException("流程定义文件不能为空且不能超过1MB");
        }
        return R.ok(definitionService.importJson(new String(file.getBytes(), StandardCharsets.UTF_8), categoryId));
    }

    @Log(title = "导出流程定义", businessType = BusinessType.EXPORT)
    @SaCheckPermission("workflow:definition:export")
    @GetMapping("/{definitionId}/export")
    public void exportJson(@PathVariable Long definitionId, HttpServletResponse response) throws IOException {
        WorkflowDefinitionVO definition = definitionService.getRequired(definitionId);
        byte[] body = definitionService.exportJson(definitionId).getBytes(StandardCharsets.UTF_8);
        String fileName = URLEncoder.encode(definition.getFlowCode() + "-v" + definition.getVersionNo(),
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".json");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @Log(title = "流程定义", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:definition:remove")
    @DeleteMapping("/{definitionId}")
    public R<Void> delete(@PathVariable Long definitionId) {
        definitionService.delete(definitionId);
        return R.ok();
    }
}
