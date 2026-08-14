package com.nz.admin.modules.system.service.online;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import com.nz.admin.framework.tenant.core.TenantConstants;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 基于 Sa-Token 的在线会话访问实现。
 */
@Component
public class SaTokenOnlineSessionAccessor implements OnlineSessionAccessor {

    @Override
    public List<String> listTokenValues() {
        return StpUtil.searchTokenValue("", 0, -1, true);
    }

    @Override
    public OnlineSessionSnapshot getSnapshot(String tokenValue) {
        Object loginId = StpUtil.getLoginIdByToken(tokenValue);
        SaSession session = StpUtil.getStpLogic().getTokenSessionByToken(tokenValue, false);
        if (loginId == null || session == null) {
            return null;
        }
        return new OnlineSessionSnapshot(
                tokenValue,
                Convert.toLong(loginId),
                Convert.toLong(session.get(TenantConstants.TOKEN_SESSION_TENANT_ID)),
                Convert.toStr(session.get(OnlineSessionKeys.TENANT_CODE)),
                Convert.toStr(session.get(OnlineSessionKeys.USERNAME)),
                Convert.toStr(session.get(OnlineSessionKeys.DEPT_NAME)),
                Convert.toStr(session.get(OnlineSessionKeys.LOGIN_IP)),
                toLocalDateTime(session.get(OnlineSessionKeys.LOGIN_TIME)),
                Convert.toStr(session.get(OnlineSessionKeys.USER_AGENT)),
                StpUtil.getStpLogic().getTokenTimeout(tokenValue)
        );
    }

    @Override
    public void logout(String tokenValue) {
        StpUtil.logoutByTokenValue(tokenValue);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        Long epochMilli = Convert.toLong(value);
        if (epochMilli == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }
}
