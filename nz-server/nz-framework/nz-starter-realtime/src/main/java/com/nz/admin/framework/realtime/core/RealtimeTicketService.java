package com.nz.admin.framework.realtime.core;

import java.util.Optional;

/**
 * 为不支持自定义请求头的浏览器长连接签发一次性票据。
 */
public interface RealtimeTicketService {

    String issue(RealtimePrincipal principal, RealtimeTransport transport);

    Optional<RealtimePrincipal> consume(String ticket, RealtimeTransport transport);

    int revokeUser(Long tenantId, Long userId);
}
