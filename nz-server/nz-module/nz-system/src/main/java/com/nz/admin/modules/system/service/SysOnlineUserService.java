package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.vo.OnlineUserVO;

import java.util.List;

/**
 * 在线用户服务。
 */
public interface SysOnlineUserService {

    /**
     * 获取当前在线用户列表。
     */
    List<OnlineUserVO> listOnlineUsers();

    /**
     * 强制下线指定 token。
     */
    void forceLogout(String tokenValue);
}
