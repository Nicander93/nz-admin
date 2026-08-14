package com.nz.admin.modules.system.service.realtime;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.realtime.config.RealtimeProperties;
import com.nz.admin.framework.realtime.core.RealtimeConnectionStats;
import com.nz.admin.framework.realtime.core.RealtimeMessage;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import com.nz.admin.framework.realtime.web.RealtimeEndpoints;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.vo.realtime.RealtimeTicketVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnBean({RealtimeTicketService.class, RealtimePublisher.class})
public class RealtimeServiceImpl implements RealtimeService {

    private final LoginUserContext loginUserContext;
    private final RealtimeTicketService ticketService;
    private final RealtimePublisher publisher;
    private final RealtimeProperties properties;

    public RealtimeServiceImpl(LoginUserContext loginUserContext,
                               RealtimeTicketService ticketService,
                               RealtimePublisher publisher,
                               RealtimeProperties properties) {
        this.loginUserContext = loginUserContext;
        this.ticketService = ticketService;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Override
    public RealtimeTicketVO issueTicket(RealtimeTransport transport) {
        RealtimePrincipal principal = currentPrincipal();
        RealtimeTicketVO result = new RealtimeTicketVO();
        result.setTicket(ticketService.issue(principal, transport));
        result.setTransport(transport);
        result.setPath(transport == RealtimeTransport.SSE
                ? RealtimeEndpoints.SSE_PATH
                : RealtimeEndpoints.WEB_SOCKET_PATH);
        result.setExpiresInSeconds(properties.getTicketTtl().toSeconds());
        return result;
    }

    @Override
    public RealtimeConnectionStats getStats() {
        return publisher.stats();
    }

    @Override
    public int sendTestMessage(String message) {
        RealtimePrincipal principal = currentPrincipal();
        return publisher.publishToUser(
                principal.tenantId(),
                principal.userId(),
                RealtimeMessage.of("test", Map.of("message", message))
        );
    }

    private RealtimePrincipal currentPrincipal() {
        Long userId = loginUserContext.getLoginUserIdOrNull();
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (userId == null || tenantId == null) {
            throw new BusinessException("当前登录身份或租户上下文无效");
        }
        return new RealtimePrincipal(userId, tenantId);
    }
}
