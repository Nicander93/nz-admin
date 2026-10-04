package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.protection.annotation.Idempotent;
import com.nz.admin.modules.workflow.engine.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Warm-Flow 的独立入口，与存量流程 API 并存。 */
@RestController
@RequestMapping("/api/workflow/engine")
public class WorkflowEngineController {
    private final org.springframework.beans.factory.ObjectProvider<WarmFlowRuntime> runtimes;
    private final org.springframework.beans.factory.ObjectProvider<com.nz.admin.common.module.NzWorkflowParticipantProvider> participants;

    public WorkflowEngineController(
            org.springframework.beans.factory.ObjectProvider<WarmFlowRuntime> runtimes,
            org.springframework.beans.factory.ObjectProvider<com.nz.admin.common.module.NzWorkflowParticipantProvider> participants) {
        this.runtimes = runtimes;
        this.participants = participants;
    }

    private WarmFlowRuntime runtime() {
        var runtime = runtimes.getIfAvailable();
        if (runtime == null) throw new com.nz.admin.common.core.BusinessException(503, "新引擎尚未启用");
        return runtime;
    }

    @GetMapping("/capabilities")
    @SaCheckPermission("workflow:engine:query")
    public R<Map<String, Boolean>> capabilities() {
        return R.ok(Map.of("enabled", runtimes.getIfAvailable() != null));
    }

    @PostMapping("/definitions")
    @SaCheckPermission("workflow:engine:design")
    @Idempotent
    public R<String> create(@Valid @RequestBody EngineDefinitionRequest request) {
        return R.ok(runtime().importDefinition(request).toString());
    }

    @GetMapping("/definitions/{id}")
    @SaCheckPermission("workflow:engine:design")
    public R<EngineDefinitionRequest> definition(@PathVariable Long id) {
        return R.ok(runtime().definition(id));
    }

    @PostMapping("/definitions/{id}/publish")
    @SaCheckPermission("workflow:engine:design")
    public R<Void> publish(@PathVariable Long id) {
        runtime().publish(id);
        return R.ok();
    }

    @GetMapping("/published-definitions")
    @SaCheckPermission("workflow:engine:query")
    public R<java.util.List<Map<String, Object>>> publishedDefinitions() { return R.ok(runtime().publishedDefinitions()); }

    @GetMapping("/center/{category}")
    @SaCheckPermission("workflow:engine:query")
    public R<java.util.List<Map<String, Object>>> center(@PathVariable String category,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return R.ok(runtime().center(category, Math.max(1, page), Math.max(1, Math.min(100, size))));
    }

    @PostMapping("/instances")
    @SaCheckPermission("workflow:engine:start")
    @Idempotent(required = true)
    public R<String> start(@Valid @RequestBody Start request) {
        return R.ok(
                runtime()
                        .start(request.flowCode(), request.businessId(), request.variables())
                        .toString());
    }

    @GetMapping("/instances/{id}")
    @SaCheckPermission("workflow:engine:query")
    public R<WarmFlowRuntime.Snapshot> get(@PathVariable Long id) {
        return R.ok(runtime().get(id));
    }

    @PostMapping("/tasks/{id}/action")
    @SaCheckPermission("workflow:engine:action")
    @Idempotent(required = true)
    public R<String> action(@PathVariable Long id, @Valid @RequestBody Action request) {
        return R.ok(
                runtime()
                        .action(id, request.type(), request.comment(), request.variables())
                        .toString());
    }

    @PostMapping("/instances/{id}/revoke")
    @SaCheckPermission("workflow:engine:start")
    @Idempotent(required = true)
    public R<String> revoke(@PathVariable Long id, @Valid @RequestBody Revoke request) {
        return R.ok(runtime().revoke(id, request.comment()).toString());
    }

    @GetMapping("/tasks/{id}/return-nodes")
    @SaCheckPermission("workflow:engine:action")
    public R<java.util.List<Map<String, String>>> returnNodes(@PathVariable Long id) { return R.ok(runtime().returnNodes(id)); }

    @PostMapping("/tasks/{id}/manage")
    @SaCheckPermission("workflow:engine:action")
    @Idempotent(required = true)
    public R<String> manage(@PathVariable Long id, @Valid @RequestBody Manage request) {
        return R.ok(runtime().manage(id, request.type(), request.targets(), request.nodeCode(), request.comment()).toString());
    }

    @GetMapping("/participants")
    @SaCheckPermission("workflow:engine:action")
    public R<com.nz.admin.common.module.NzWorkflowParticipantProvider.Selection> participants(
            @RequestParam(defaultValue = "") String name) {
        var provider = participants.getIfAvailable();
        return R.ok(provider == null ? new com.nz.admin.common.module.NzWorkflowParticipantProvider.Selection(java.util.List.of(), 0)
                : provider.select("user", "", name, 1, 100));
    }

    @DeleteMapping("/instances/{id}")
    @SaCheckPermission("workflow:engine:design")
    public R<Void> deleteInstance(@PathVariable Long id) {
        runtime().deleteInstance(id);
        return R.ok();
    }

    @DeleteMapping("/definitions/{id}")
    @SaCheckPermission("workflow:engine:design")
    public R<Void> deleteDefinition(@PathVariable Long id) {
        runtime().deleteDefinition(id);
        return R.ok();
    }

    public record Revoke(@Size(max = 1000) String comment) {}
    public record Manage(@NotBlank @Pattern(regexp = "TRANSFER|DEPUTE|ADD|REDUCE|RETURN|TERMINATE") String type,
                         @Size(max = 20) java.util.List<@Pattern(regexp = "[1-9][0-9]*") String> targets,
                         @Size(max = 64) String nodeCode, @Size(max = 1000) String comment) {}

    public record Start(
            @NotBlank @Size(max = 64) String flowCode,
            @NotBlank @Size(max = 128) String businessId,
            @Size(max = 100) Map<String, Object> variables) {}

    public record Action(
            @NotBlank @Pattern(regexp = "PASS|REJECT") String type,
            @Size(max = 1000) String comment,
            @Size(max = 100) Map<String, Object> variables) {}
}
