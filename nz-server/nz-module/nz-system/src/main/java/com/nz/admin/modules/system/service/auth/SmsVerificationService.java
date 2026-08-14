package com.nz.admin.modules.system.service.auth;

import com.nz.admin.modules.system.entity.dataobject.user.UserDO;

/** 短信登录验证码服务。 */
public interface SmsVerificationService {

    void sendLoginCode(String phone);

    UserDO verifyLoginCode(String phone, String code);
}
