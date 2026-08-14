package com.nz.admin.framework.realtime.web;

import com.nz.admin.framework.realtime.core.RealtimeConnectionRegistry;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 使用一次性票据建立 SSE 连接。
 */
@RestController
public class RealtimeSseController {

    private final RealtimeTicketService ticketService;
    private final RealtimeConnectionRegistry connectionRegistry;

    public RealtimeSseController(RealtimeTicketService ticketService,
                                 RealtimeConnectionRegistry connectionRegistry) {
        this.ticketService = ticketService;
        this.connectionRegistry = connectionRegistry;
    }

    @GetMapping(value = RealtimeEndpoints.SSE_PATH, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String ticket) {
        RealtimePrincipal principal = ticketService.consume(ticket, RealtimeTransport.SSE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "实时连接票据无效或已过期"
                ));
        return connectionRegistry.connectSse(principal);
    }
}
