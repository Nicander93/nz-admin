package com.nz.admin.modules.system.entity.vo.social;

import java.time.LocalDateTime;

/** 当前用户已绑定的第三方账号。 */
public record SocialBindingVO(
        Long id,
        String provider,
        String providerName,
        String username,
        String nickname,
        String email,
        String avatar,
        LocalDateTime bindTime
) {
}
