package com.nz.admin.modules.system.controller.social;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.social.core.SocialAuthorization;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.vo.social.SocialBindingVO;
import com.nz.admin.modules.system.service.social.SocialAccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 当前用户的第三方账号绑定管理。 */
@RestController
@RequestMapping("/api/system/social")
public class SocialAccountController {

    private final SocialAccountService socialAccountService;

    public SocialAccountController(SocialAccountService socialAccountService) {
        this.socialAccountService = socialAccountService;
    }

    @SaCheckPermission("system:social:list")
    @GetMapping("/list")
    public R<List<SocialBindingVO>> list() {
        return R.ok(socialAccountService.listCurrent(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckPermission("system:social:bind")
    @PostMapping("/authorize/{provider}")
    public R<SocialAuthorization> authorize(@PathVariable String provider) {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) {
            throw new BusinessException("当前租户上下文不存在");
        }
        return R.ok(socialAccountService.authorizeBinding(
                tenantId, StpUtil.getLoginIdAsLong(), provider));
    }

    @Log(title = "第三方账号", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:social:remove")
    @DeleteMapping("/{bindingId}")
    public R<Void> unbind(@PathVariable Long bindingId) {
        socialAccountService.unbind(StpUtil.getLoginIdAsLong(), bindingId);
        return R.ok();
    }
}
