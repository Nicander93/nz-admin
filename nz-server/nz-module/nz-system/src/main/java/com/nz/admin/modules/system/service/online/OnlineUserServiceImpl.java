package com.nz.admin.modules.system.service.online;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.realtime.core.RealtimeConnectionManager;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.vo.online.OnlineUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 在线用户服务实现。
 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private final OnlineSessionAccessor sessionAccessor;
    private final TenantProperties tenantProperties;

    @Autowired(required = false)
    private RealtimeConnectionManager realtimeConnectionManager;
    @Autowired(required = false)
    private RealtimeTicketService realtimeTicketService;

    public OnlineUserServiceImpl(OnlineSessionAccessor sessionAccessor, TenantProperties tenantProperties) {
        this.sessionAccessor = sessionAccessor;
        this.tenantProperties = tenantProperties;
    }

    @Override
    public List<OnlineUserVO> listOnlineUsers(String username, String loginIp) {
        Long currentTenantId = TenantContextHolder.getTenantIdOrNull();
        if (currentTenantId == null) {
            return List.of();
        }
        boolean canViewAllTenants = tenantProperties.getDefaultTenantId().equals(currentTenantId);
        return sessionAccessor.listTokenValues().stream()
                .map(sessionAccessor::getSnapshot)
                .filter(snapshot -> snapshot != null && canAccess(snapshot, currentTenantId, canViewAllTenants))
                .filter(snapshot -> StrUtil.isBlank(username)
                        || StrUtil.containsIgnoreCase(snapshot.username(), username))
                .filter(snapshot -> StrUtil.isBlank(loginIp)
                        || StrUtil.containsIgnoreCase(snapshot.loginIp(), loginIp))
                .map(this::toVO)
                .toList();
    }

    @Override
    public void forceLogout(String tokenValue) {
        if (StrUtil.isBlank(tokenValue)) {
            throw new BusinessException("在线会话不存在或已失效");
        }
        OnlineSessionSnapshot snapshot = sessionAccessor.getSnapshot(tokenValue);
        Long currentTenantId = TenantContextHolder.getTenantIdOrNull();
        boolean canViewAllTenants = currentTenantId != null
                && tenantProperties.getDefaultTenantId().equals(currentTenantId);
        if (snapshot == null || currentTenantId == null
                || !canAccess(snapshot, currentTenantId, canViewAllTenants)) {
            throw new BusinessException("在线会话不存在或无权操作");
        }
        if (realtimeTicketService != null) {
            realtimeTicketService.revokeUser(snapshot.tenantId(), snapshot.userId());
        }
        if (realtimeConnectionManager != null) {
            realtimeConnectionManager.disconnectUser(snapshot.tenantId(), snapshot.userId());
        }
        sessionAccessor.logout(tokenValue);
    }

    private boolean canAccess(OnlineSessionSnapshot snapshot,
                              Long currentTenantId,
                              boolean canViewAllTenants) {
        return canViewAllTenants || currentTenantId.equals(snapshot.tenantId());
    }

    private OnlineUserVO toVO(OnlineSessionSnapshot snapshot) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setTokenValue(snapshot.tokenValue());
        vo.setUserId(snapshot.userId());
        vo.setTenantId(snapshot.tenantId());
        vo.setTenantCode(snapshot.tenantCode());
        vo.setUsername(snapshot.username());
        vo.setDeptName(snapshot.deptName());
        vo.setLoginIp(snapshot.loginIp());
        vo.setLoginTime(snapshot.loginTime());
        vo.setUserAgent(snapshot.userAgent());
        vo.setTokenTimeout(snapshot.tokenTimeout());
        return vo;
    }
}
