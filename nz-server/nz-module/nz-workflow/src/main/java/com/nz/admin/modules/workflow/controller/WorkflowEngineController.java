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

    public WorkflowEngineController(
            org.springframework.beans.factory.ObjectProvider<WarmFlowRuntime> runtimes) {
        this.runtimes = runtimes;
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

    public record Start(
            @NotBlank @Size(max = 64) String flowCode,
            @NotBlank @Size(max = 128) String businessId,
            @Size(max = 100) Map<String, Object> variables) {}

    public record Action(
            @NotBlank @Pattern(regexp = "PASS|REJECT") String type,
            @Size(max = 1000) String comment,
            @Size(max = 100) Map<String, Object> variables) {}
}
