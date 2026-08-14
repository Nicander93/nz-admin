package com.nz.admin.modules.system.service.realtime;

import com.nz.admin.framework.realtime.core.RealtimeConnectionStats;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import com.nz.admin.modules.system.entity.vo.realtime.RealtimeTicketVO;

public interface RealtimeService {

    RealtimeTicketVO issueTicket(RealtimeTransport transport);

    RealtimeConnectionStats getStats();

    int sendTestMessage(String message);
}
