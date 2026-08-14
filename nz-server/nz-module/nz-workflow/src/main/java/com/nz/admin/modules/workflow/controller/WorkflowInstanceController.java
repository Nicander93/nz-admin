package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceCommentRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceUrgeRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceStartRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowInstanceVO;
import com.nz.admin.modules.workflow.service.WorkflowInstanceService;
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

/** 流程实例接口。 */
@RestController
@RequestMapping("/api/workflow/instance")
public class WorkflowInstanceController {

    private final WorkflowInstanceService instanceService;

    public WorkflowInstanceController(WorkflowInstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @SaCheckPermission("workflow:instance:list")
    @GetMapping("/page")
    public R<PageResult<WorkflowInstanceVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String flowCode,
                                                   @RequestParam(required = false) String title,
                                                   @RequestParam(required = false) String businessKey,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(instanceService.page(pageNum, pageSize, flowCode, title, businessKey, status, mine));
    }

    @SaCheckPermission("workflow:instance:query")
    @GetMapping("/{instanceId}")
    public R<WorkflowInstanceVO> get(@PathVariable Long instanceId) {
        return R.ok(instanceService.getRequired(instanceId));
    }

    @RepeatSubmit
    @Log(title = "发起流程实例", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:instance:start")
    @PostMapping("/start")
    public R<Long> start(@Valid @RequestBody WorkflowInstanceStartRequest request) {
        return R.ok(instanceService.start(request));
    }

    @RepeatSubmit
    @Log(title = "办理流程实例", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:instance:action")
    @PostMapping("/{instanceId}/action")
    public R<Void> action(@PathVariable Long instanceId,
                          @Valid @RequestBody WorkflowInstanceActionRequest request) {
        instanceService.action(instanceId, request);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "撤回流程实例", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:instance:cancel")
    @PostMapping("/{instanceId}/cancel")
    public R<Void> cancel(@PathVariable Long instanceId,
                          @Valid @RequestBody WorkflowInstanceCommentRequest request) {
        instanceService.cancel(instanceId, request.getComment());
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "终止流程实例", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:instance:terminate")
    @PostMapping("/{instanceId}/terminate")
    public R<Void> terminate(@PathVariable Long instanceId,
                             @Valid @RequestBody WorkflowInstanceCommentRequest request) {
        instanceService.terminate(instanceId, request.getComment());
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "催办流程实例", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:instance:urge")
    @PostMapping("/{instanceId}/urge")
    public R<Void> urge(@PathVariable Long instanceId,
                        @Valid @RequestBody WorkflowInstanceUrgeRequest request) {
        instanceService.urge(instanceId, request.getContent());
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "流程实例运行状态", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:instance:active")
    @PutMapping("/{instanceId}/active")
    public R<Void> setActive(@PathVariable Long instanceId, @RequestParam boolean active) {
        instanceService.setActive(instanceId, active);
        return R.ok();
    }

    @Log(title = "流程实例", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:instance:remove")
    @DeleteMapping("/{instanceId}")
    public R<Void> delete(@PathVariable Long instanceId) {
        instanceService.delete(instanceId);
        return R.ok();
    }
}
