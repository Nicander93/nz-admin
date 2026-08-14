package com.nz.admin.modules.system.controller.realtime;

import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.realtime.core.RealtimeConnectionStats;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import com.nz.admin.modules.system.entity.dto.realtime.RealtimeTestRequest;
import com.nz.admin.modules.system.entity.vo.realtime.RealtimeTicketVO;
import com.nz.admin.modules.system.service.realtime.RealtimeService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/realtime")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(RealtimeService.class)
public class RealtimeController {

    private final RealtimeService realtimeService;

    public RealtimeController(RealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @GetMapping("/ticket")
    @SaCheckPermission("system:realtime:view")
    public R<RealtimeTicketVO> issueTicket(
            @RequestParam(defaultValue = "SSE") RealtimeTransport transport) {
        return R.ok(realtimeService.issueTicket(transport));
    }

    @GetMapping("/stats")
    @SaCheckPermission("system:realtime:view")
    public R<RealtimeConnectionStats> stats() {
        return R.ok(realtimeService.getStats());
    }

    @PostMapping("/test")
    @SaCheckPermission("system:realtime:send")
    @Log(title = "实时通信测试", businessType = BusinessType.OTHER)
    public R<Integer> sendTest(@Valid @RequestBody RealtimeTestRequest request) {
        return R.ok(realtimeService.sendTestMessage(request.getMessage()));
    }
}
