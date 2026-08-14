package com.nz.admin.modules.system.service.auth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.core.TenantConstants;
import com.nz.admin.modules.system.entity.dataobject.client.ClientDO;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.client.ClientService;
import com.nz.admin.modules.system.service.dept.DeptService;
import com.nz.admin.modules.system.service.log.LoginLogService;
import com.nz.admin.modules.system.service.online.OnlineSessionKeys;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/** 登录授权服务实现。 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;
    private final ClientService clientService;
    private final DeptService deptService;
    private final LoginLogService loginLogService;
    private final Optional<SmsVerificationService> smsVerificationService;

    public AuthenticationServiceImpl(UserService userService, ClientService clientService,
                                     DeptService deptService, LoginLogService loginLogService,
                                     Optional<SmsVerificationService> smsVerificationService) {
        this.userService = userService;
        this.clientService = clientService;
        this.deptService = deptService;
        this.loginLogService = loginLogService;
        this.smsVerificationService = smsVerificationService;
    }

    @Override
    public String loginByPassword(TenantDO tenant, String clientId, String username,
                                  String password, LoginMetadata metadata) {
        ClientDO client = clientService.getEnabledForLogin(clientId, "account");
        UserDO user = userService.getByUsername(username);
        if (user == null || StrUtil.isBlank(user.getPassword())
                || !BCrypt.checkpw(password, user.getPassword())) {
            saveLoginFail(username, null, metadata, "用户名或密码错误");
            throw new BusinessException("用户名或密码错误");
        }
        validateUser(user, metadata);
        return completeLogin(user, tenant, client, "account", metadata);
    }

    @Override
    public void sendSmsLoginCode(String clientId, String phone) {
        clientService.getEnabledForLogin(clientId, "sms");
        requireSmsVerificationService().sendLoginCode(phone);
    }

    @Override
    public String loginBySms(TenantDO tenant, String clientId, String phone,
                             String code, LoginMetadata metadata) {
        ClientDO client = clientService.getEnabledForLogin(clientId, "sms");
        UserDO user = requireSmsVerificationService().verifyLoginCode(phone, code);
        validateUser(user, metadata);
        return completeLogin(user, tenant, client, "sms", metadata);
    }

    @Override
    public String loginBySocial(TenantDO tenant, String clientId, UserDO user,
                                LoginMetadata metadata) {
        ClientDO client = clientService.getEnabledForLogin(clientId, "social");
        validateUser(user, metadata);
        return completeLogin(user, tenant, client, "social", metadata);
    }

    private void validateUser(UserDO user, LoginMetadata metadata) {
        if (user.getStatus() != null && user.getStatus() != 0) {
            saveLoginFail(user.getUsername(), user.getId(), metadata, "账号已被禁用");
            throw new BusinessException("账号已被禁用");
        }
    }

    private String completeLogin(UserDO user, TenantDO tenant, ClientDO client,
                                 String loginType, LoginMetadata metadata) {
        StpUtil.login(user.getId(), new SaLoginModel()
                .setTimeout(client.getTokenTimeout())
                .setDevice(client.getClientId()));
        LocalDateTime loginTime = LocalDateTime.now();
        DeptDO dept = user.getDeptId() == null ? null : deptService.getById(user.getDeptId());
        StpUtil.getTokenSession()
                .set(TenantConstants.TOKEN_SESSION_TENANT_ID, tenant.getId())
                .set(OnlineSessionKeys.TENANT_CODE, tenant.getTenantCode())
                .set(OnlineSessionKeys.USERNAME, user.getUsername())
                .set(OnlineSessionKeys.DEPT_NAME, dept == null ? "" : dept.getName())
                .set(OnlineSessionKeys.LOGIN_IP, metadata.ip())
                .set(OnlineSessionKeys.LOGIN_TIME, loginTime)
                .set(OnlineSessionKeys.USER_AGENT, metadata.userAgent())
                .set(OnlineSessionKeys.CLIENT_ID, client.getClientId())
                .set(OnlineSessionKeys.LOGIN_TYPE, loginType);
        loginLogService.saveAsync(new LoginLogDO()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setIp(metadata.ip())
                .setStatus(0)
                .setMsg(switch (loginType) {
                    case "sms" -> "短信登录成功";
                    case "social" -> "第三方账号登录成功";
                    default -> "登录成功";
                })
                .setLoginTime(loginTime));
        return StpUtil.getTokenValue();
    }

    private SmsVerificationService requireSmsVerificationService() {
        return smsVerificationService.orElseThrow(() -> new BusinessException("短信验证码登录未启用"));
    }

    private void saveLoginFail(String username, Long userId, LoginMetadata metadata, String message) {
        loginLogService.saveAsync(new LoginLogDO()
                .setUserId(userId)
                .setUsername(StrUtil.blankToDefault(username, "unknown"))
                .setIp(metadata.ip())
                .setStatus(1)
                .setMsg(message)
                .setLoginTime(LocalDateTime.now()));
    }
}
