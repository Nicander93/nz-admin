package com.nz.admin.modules.demo.controller;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.protection.annotation.Idempotent;
import com.nz.admin.modules.demo.service.LeaveApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 业务表单入口，无需用户填写流程实例 ID。 */
@RestController
@RequestMapping("/api/demo/leave")
public class LeaveApplicationController {
    private final LeaveApplicationService service;
    public LeaveApplicationController(LeaveApplicationService service) { this.service = service; }
    @GetMapping
    @SaCheckPermission("demo:leave:query")
    public R<List<Map<String, Object>>> list() { return R.ok(service.list()); }
    @GetMapping("/flows")
    @SaCheckPermission("demo:leave:query")
    public R<java.util.List<com.nz.admin.common.module.NzWorkflowBusinessLauncher.Definition>> flows() { return R.ok(service.flows()); }
    @PostMapping
    @SaCheckPermission("demo:leave:save")
    @Idempotent(required = true)
    public R<String> create(@Valid @RequestBody Draft request) {
        return R.ok(service.save(null, request.reason(), request.startDate(), request.endDate(), request.flowCode()));
    }
    @PutMapping("/{id}")
    @SaCheckPermission("demo:leave:save")
    public R<String> update(@PathVariable String id, @Valid @RequestBody Draft request) {
        return R.ok(service.save(id, request.reason(), request.startDate(), request.endDate(), request.flowCode()));
    }
    @PostMapping("/{id}/submit")
    @SaCheckPermission("demo:leave:submit")
    @Idempotent(required = true)
    public R<String> submit(@PathVariable String id) { return R.ok(service.submit(id)); }
    public record Draft(@NotBlank @Size(max = 1000) String reason, @NotNull LocalDate startDate,
                        @NotNull LocalDate endDate, @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,63}") String flowCode) {}
}
