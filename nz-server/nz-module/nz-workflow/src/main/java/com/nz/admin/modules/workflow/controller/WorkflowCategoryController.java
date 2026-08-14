package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.excel.support.ExcelResponseUtils;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.modules.workflow.convert.WorkflowCategoryConvert;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryCreateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowCategoryUpdateRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryVO;
import com.nz.admin.modules.workflow.service.WorkflowCategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 流程分类接口。
 */
@RestController
@RequestMapping("/api/workflow/category")
public class WorkflowCategoryController {

    private final WorkflowCategoryService categoryService;

    public WorkflowCategoryController(WorkflowCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @SaCheckPermission("workflow:category:list")
    @GetMapping("/list")
    public R<List<WorkflowCategoryVO>> list(@RequestParam(required = false) String categoryName,
                                             @RequestParam(required = false) Long parentId) {
        return R.ok(categoryService.list(categoryName, parentId).stream()
                .map(WorkflowCategoryConvert::toVO)
                .toList());
    }

    @GetMapping("/tree")
    public R<List<WorkflowCategoryVO>> tree(@RequestParam(required = false) String categoryName) {
        return R.ok(categoryService.tree(categoryName));
    }

    @SaCheckPermission("workflow:category:query")
    @GetMapping("/{categoryId}")
    public R<WorkflowCategoryVO> get(@PathVariable Long categoryId) {
        return R.ok(WorkflowCategoryConvert.toVO(categoryService.getRequired(categoryId)));
    }

    @RepeatSubmit
    @Log(title = "流程分类", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:category:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody WorkflowCategoryCreateRequest request) {
        return R.ok(categoryService.create(request));
    }

    @RepeatSubmit
    @Log(title = "流程分类", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:category:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody WorkflowCategoryUpdateRequest request) {
        categoryService.update(request);
        return R.ok();
    }

    @Log(title = "流程分类", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:category:remove")
    @DeleteMapping("/{categoryId}")
    public R<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return R.ok();
    }

    @SaCheckPermission("workflow:category:export")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String categoryName,
                       @RequestParam(required = false) Long parentId,
                       HttpServletResponse response) throws IOException {
        ExcelResponseUtils.write(response, "workflow-category", "流程分类",
                com.nz.admin.modules.workflow.entity.vo.WorkflowCategoryExportVO.class,
                WorkflowCategoryConvert.toExportList(categoryService.list(categoryName, parentId)));
    }
}
