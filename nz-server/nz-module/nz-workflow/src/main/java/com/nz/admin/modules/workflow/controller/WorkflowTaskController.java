package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.protection.annotation.RepeatSubmit;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceActionRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowInstanceCommentRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskCopyRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskDelegateRequest;
import com.nz.admin.modules.workflow.entity.dto.WorkflowTaskTransferRequest;
import com.nz.admin.modules.workflow.entity.vo.WorkflowTaskVO;
import com.nz.admin.modules.workflow.service.WorkflowTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 流程任务接口。 */
@RestController
@RequestMapping("/api/workflow/task")
public class WorkflowTaskController {

    private final WorkflowTaskService taskService;

    public WorkflowTaskController(WorkflowTaskService taskService) {
        this.taskService = taskService;
    }

    @SaCheckPermission("workflow:task:list")
    @GetMapping("/todo/page")
    public R<PageResult<WorkflowTaskVO>> pageTodo(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String nodeName) {
        return R.ok(taskService.pageTodo(pageNum, pageSize, nodeName));
    }

    @SaCheckPermission("workflow:task:list")
    @GetMapping("/done/page")
    public R<PageResult<WorkflowTaskVO>> pageDone(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String nodeName,
                                                   @RequestParam(required = false) String action) {
        return R.ok(taskService.pageDone(pageNum, pageSize, nodeName, action));
    }

    @SaCheckPermission("workflow:task:list")
    @GetMapping("/copy/page")
    public R<PageResult<WorkflowTaskVO>> pageCopy(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) Integer readStatus) {
        return R.ok(taskService.pageCopy(pageNum, pageSize, readStatus));
    }

    @SaCheckPermission("workflow:task:query")
    @GetMapping("/{taskId}")
    public R<WorkflowTaskVO> get(@PathVariable Long taskId) {
        return R.ok(taskService.getRequired(taskId));
    }

    @RepeatSubmit
    @Log(title = "办理流程任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:task:action")
    @PostMapping("/{taskId}/action")
    public R<Void> action(@PathVariable Long taskId,
                          @Valid @RequestBody WorkflowInstanceActionRequest request) {
        taskService.action(taskId, request);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "转办流程任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:task:transfer")
    @PostMapping("/{taskId}/transfer")
    public R<Void> transfer(@PathVariable Long taskId,
                            @Valid @RequestBody WorkflowTaskTransferRequest request) {
        taskService.transfer(taskId, request);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "委派流程任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:task:delegate")
    @PostMapping("/{taskId}/delegate")
    public R<Void> delegate(@PathVariable Long taskId,
                            @Valid @RequestBody WorkflowTaskDelegateRequest request) {
        taskService.delegate(taskId, request);
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "完成委派任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:task:delegate")
    @PostMapping("/{taskId}/resolve")
    public R<Void> resolveDelegation(@PathVariable Long taskId,
                                     @Valid @RequestBody WorkflowInstanceCommentRequest request) {
        taskService.resolveDelegation(taskId, request.getComment());
        return R.ok();
    }

    @RepeatSubmit
    @Log(title = "抄送流程任务", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:task:copy")
    @PostMapping("/{taskId}/copy")
    public R<Void> copy(@PathVariable Long taskId,
                        @Valid @RequestBody WorkflowTaskCopyRequest request) {
        taskService.copy(taskId, request);
        return R.ok();
    }

    @SaCheckPermission("workflow:task:read")
    @PutMapping("/copy/{copyId}/read")
    public R<Void> markCopyRead(@PathVariable Long copyId) {
        taskService.markCopyRead(copyId);
        return R.ok();
    }
}
