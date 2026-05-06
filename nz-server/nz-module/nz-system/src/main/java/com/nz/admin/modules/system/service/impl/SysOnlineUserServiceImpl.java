package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.system.service.SysOnlineUserService;
import com.nz.admin.modules.system.vo.OnlineUserVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 在线用户服务实现。
 */
@Service
public class SysOnlineUserServiceImpl implements SysOnlineUserService {

    @Override
    public List<OnlineUserVO> listOnlineUsers() {
        // 使用 Sa-Token 获取在线用户列表
        List<String> tokenValues = cn.dev33.satoken.stp.StpUtil.searchTokenValue("", 0, -1, true);
        List<OnlineUserVO> result = new ArrayList<>();
        for (String tokenValue : tokenValues) {
            OnlineUserVO vo = new OnlineUserVO();
            vo.setTokenValue(tokenValue);
            // 从 token 中获取登录信息
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdByToken(tokenValue);
            if (loginId != null) {
                vo.setUserId(Long.valueOf(loginId.toString()));
            }
            // 获取会话中的自定义数据
            Object username = cn.dev33.satoken.stp.StpUtil.getTokenSessionByToken(tokenValue).get("username");
            if (username != null) {
                vo.setUsername(username.toString());
            }
            Object deptName = cn.dev33.satoken.stp.StpUtil.getTokenSessionByToken(tokenValue).get("deptName");
            if (deptName != null) {
                vo.setDeptName(deptName.toString());
            }
            Object loginIp = cn.dev33.satoken.stp.StpUtil.getTokenSessionByToken(tokenValue).get("loginIp");
            if (loginIp != null) {
                vo.setLoginIp(loginIp.toString());
            }
            Object loginTime = cn.dev33.satoken.stp.StpUtil.getTokenSessionByToken(tokenValue).get("loginTime");
            if (loginTime != null) {
                vo.setLoginTime((LocalDateTime) loginTime);
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void forceLogout(String tokenValue) {
        if (StrUtil.isNotBlank(tokenValue)) {
            cn.dev33.satoken.stp.StpUtil.logoutByTokenValue(tokenValue);
        }
    }
}
