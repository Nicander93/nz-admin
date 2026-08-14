package com.nz.admin.modules.system.service.profile;

import com.nz.admin.modules.system.entity.dto.profile.PasswordUpdateRequest;
import com.nz.admin.modules.system.entity.dto.profile.ProfileUpdateRequest;
import com.nz.admin.modules.system.entity.vo.profile.ProfileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 当前登录用户的个人资料服务。 */
public interface ProfileService {

    ProfileVO getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequest request);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    Long updateAvatar(Long userId, MultipartFile file) throws IOException;

    Long getAvatarFileId(Long userId);
}
