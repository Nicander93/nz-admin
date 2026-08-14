package com.nz.admin.modules.system.entity.vo.realtime;

import com.nz.admin.framework.realtime.core.RealtimeTransport;
import lombok.Data;

@Data
public class RealtimeTicketVO {

    private String ticket;
    private RealtimeTransport transport;
    private String path;
    private long expiresInSeconds;
}
