package com.nz.admin.modules.system.entity.vo.profile;

import java.time.LocalDateTime;

/** 当前用户个人中心信息。 */
public record ProfileVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        String gender,
        Long avatarFileId,
        String roleGroup,
        String postGroup,
        LocalDateTime createTime
) {
}
