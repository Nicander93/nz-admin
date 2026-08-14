package com.nz.admin.modules.system.service.online;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.tenant.core.TenantConstants;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SaTokenOnlineSessionAccessorTest {

    @Test
    void readsSnapshotWithoutCreatingMissingSessions() {
        SaSession session = mock(SaSession.class);
        StpLogic stpLogic = mock(StpLogic.class);
        LocalDateTime loginTime = LocalDateTime.of(2026, 8, 11, 9, 30);
        when(stpLogic.getTokenSessionByToken("token-a", false)).thenReturn(session);
        when(stpLogic.getTokenTimeout("token-a")).thenReturn(1800L);
        when(session.get(TenantConstants.TOKEN_SESSION_TENANT_ID)).thenReturn(2L);
        when(session.get(OnlineSessionKeys.TENANT_CODE)).thenReturn("tenant-2");
        when(session.get(OnlineSessionKeys.USERNAME)).thenReturn("alice");
        when(session.get(OnlineSessionKeys.DEPT_NAME)).thenReturn("研发部");
        when(session.get(OnlineSessionKeys.LOGIN_IP)).thenReturn("10.0.0.2");
        when(session.get(OnlineSessionKeys.LOGIN_TIME)).thenReturn(loginTime);
        when(session.get(OnlineSessionKeys.USER_AGENT)).thenReturn("Mozilla/5.0");

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginIdByToken("token-a")).thenReturn(20L);
            stpUtil.when(StpUtil::getStpLogic).thenReturn(stpLogic);

            OnlineSessionSnapshot snapshot = new SaTokenOnlineSessionAccessor().getSnapshot("token-a");

            assertThat(snapshot.userId()).isEqualTo(20L);
            assertThat(snapshot.tenantId()).isEqualTo(2L);
            assertThat(snapshot.username()).isEqualTo("alice");
            assertThat(snapshot.loginTime()).isEqualTo(loginTime);
            assertThat(snapshot.tokenTimeout()).isEqualTo(1800L);
        }
    }
}
