package com.nz.admin.framework.realtime.web;

import com.nz.admin.framework.realtime.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import java.util.HashMap;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RealtimeHandshakeInterceptorTest {
    @Test
    void invalidTicketUsesUnauthorizedHandshakeStatus() {
        RealtimeTicketService tickets = mock(RealtimeTicketService.class);
        when(tickets.consume("stale", RealtimeTransport.WEBSOCKET)).thenReturn(Optional.empty());
        var request = new MockHttpServletRequest("GET", "/realtime/ws");
        request.setQueryString("ticket=stale");
        var response = new MockHttpServletResponse();
        var attributes = new HashMap<String,Object>();
        assertThat(new RealtimeHandshakeInterceptor(tickets).beforeHandshake(new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response), mock(WebSocketHandler.class), attributes)).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(attributes).isEmpty();
    }
}
