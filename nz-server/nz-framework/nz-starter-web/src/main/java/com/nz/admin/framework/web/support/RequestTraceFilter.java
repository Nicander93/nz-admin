package com.nz.admin.framework.web.support;

import com.nz.admin.framework.web.properties.WebFrameworkProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求 TraceId 过滤器。
 */
public class RequestTraceFilter extends OncePerRequestFilter {

    private final WebFrameworkProperties.Trace properties;

    public RequestTraceFilter(WebFrameworkProperties.Trace properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(properties.getRequestHeaderName());
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(properties.getMdcKey(), traceId);
        response.setHeader(properties.getResponseHeaderName(), traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(properties.getMdcKey());
        }
    }
}
