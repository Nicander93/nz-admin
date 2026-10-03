package com.nz.admin.framework.tenant.web;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.tenant.core.TenantConstants;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从服务端令牌会话恢复租户，不读取可伪造的租户请求头。
 */
@lombok.extern.slf4j.Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = StpUtil.getTokenValue();
            if (StrUtil.isNotBlank(token)) {
                SaSession session = StpUtil.getTokenSessionByToken(token);
                if (session != null) {
                    Object tenantId = session.get(TenantConstants.TOKEN_SESSION_TENANT_ID);
                    if (tenantId != null) {
                        TenantContextHolder.setTenantId(Long.valueOf(tenantId.toString()));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            log.warn("令牌租户上下文恢复失败", ignored);
            TenantContextHolder.clear();
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":503,\"msg\":\"登录状态暂不可用\"}");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
