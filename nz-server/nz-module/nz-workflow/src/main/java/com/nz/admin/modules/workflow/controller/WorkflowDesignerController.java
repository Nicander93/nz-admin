package com.nz.admin.modules.workflow.controller;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.R;
import com.nz.admin.common.module.NzWorkflowParticipantProvider;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.protection.annotation.Idempotent;
import com.nz.admin.modules.workflow.engine.WorkflowDesignerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.dromara.warm.flow.core.dto.DefJson;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 仅复用上游静态画布，不装配无权限注解的上游保存控制器。 */
@RestController
@RequestMapping("/api/workflow/designer")
@SaCheckPermission("workflow:engine:design")
public class WorkflowDesignerController {
    private final ObjectProvider<WorkflowDesignerService> services;
    private final ObjectProvider<NzWorkflowParticipantProvider> participants;
    public WorkflowDesignerController(ObjectProvider<WorkflowDesignerService> services,
                                      ObjectProvider<NzWorkflowParticipantProvider> participants) {
        this.services = services;
        this.participants = participants;
    }
    private WorkflowDesignerService service() {
        var result = services.getIfAvailable();
        if (result == null) throw new BusinessException(503, "新引擎尚未启用");
        return result;
    }
    @GetMapping("/business-types")
    public R<List<Map<String, String>>> businessTypes() { return R.ok(service().businessTypes()); }
    @GetMapping("/definitions")
    public R<List<Map<String, Object>>> list() { return R.ok(service().definitions()); }
    @PostMapping("/definitions")
    @Idempotent(required = true)
    public R<String> create(@Valid @RequestBody Draft request) {
        return R.ok(service().create(request.flowCode(), request.flowName(), request.sourceId(), request.businessType()).toString());
    }
    @GetMapping("/warm-flow/query-def/{id}")
    public R<DefJson> query(@PathVariable Long id) { return R.ok(service().query(id)); }
    @PostMapping("/warm-flow/save-json")
    public R<Void> save(@RequestBody DefJson request) { service().save(request); return R.ok(); }
    @GetMapping("/warm-flow-ui/config")
    public R<Map<String, Object>> config() {
        return R.ok(Map.of("tokenNameList", List.of("Authorization"), "framework", "SPRINGBOOT"));
    }
    @GetMapping("/warm-flow/handler-type")
    public R<List<String>> types() { return R.ok(List.of("用户", "角色")); }
    @GetMapping("/warm-flow/handler-result")
    public R<Map<String, Object>> handlers(@RequestParam(defaultValue = "用户") String handlerType,
            @RequestParam(defaultValue = "") String handlerCode, @RequestParam(defaultValue = "") String handlerName,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        var provider = participants.getIfAvailable();
        var result = provider == null ? new NzWorkflowParticipantProvider.Selection(List.of(), 0)
                : provider.select("角色".equals(handlerType) ? "role" : "user", handlerCode, handlerName,
                        Math.max(1, pageNum), Math.max(1, Math.min(100, pageSize)));
        return R.ok(Map.of("handlerAuths", Map.of("rows", result.list(), "total", result.total()), "treeSelections", List.of()));
    }
    @GetMapping("/warm-flow/handler-feedback")
    public R<List<NzWorkflowParticipantProvider.Participant>> feedback(@RequestParam MultiValueMap<String, String> query) {
        var ids = query.entrySet().stream()
                .filter(entry -> entry.getKey().matches("storageIds(?:\\[\\d*\\])?"))
                .flatMap(entry -> entry.getValue().stream()).toList();
        if (ids.size() > 100) throw new BusinessException("选择的办理人过多");
        var provider = participants.getIfAvailable();
        return R.ok(provider == null ? List.of() : provider.feedback(ids));
    }
    @GetMapping({"/warm-flow/node-ext", "/warm-flow/listener-list"})
    public R<List<Object>> extensions() { return R.ok(List.of()); }
    public record Draft(@NotBlank @Size(max = 64) String flowCode,
                        @NotBlank @Size(max = 100) String flowName, Long sourceId, @Size(max = 64) String businessType) {}
}
