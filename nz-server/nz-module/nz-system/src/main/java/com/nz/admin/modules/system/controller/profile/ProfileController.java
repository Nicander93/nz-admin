package com.nz.admin.modules.system.controller.profile;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dto.profile.PasswordUpdateRequest;
import com.nz.admin.modules.system.entity.dto.profile.ProfileUpdateRequest;
import com.nz.admin.modules.system.entity.vo.profile.ProfileVO;
import com.nz.admin.modules.system.service.file.FileService;
import com.nz.admin.modules.system.service.profile.ProfileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 当前登录用户个人中心接口。 */
@RestController
@RequestMapping("/api/system/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final FileService fileService;

    public ProfileController(ProfileService profileService, FileService fileService) {
        this.profileService = profileService;
        this.fileService = fileService;
    }

    @GetMapping
    public R<ProfileVO> getProfile() {
        return R.ok(profileService.getProfile(StpUtil.getLoginIdAsLong()));
    }

    @Log(title = "个人资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        profileService.updateProfile(StpUtil.getLoginIdAsLong(), request);
        return R.ok();
    }

    @Log(title = "修改密码", businessType = BusinessType.UPDATE)
    @PutMapping("/password")
    public R<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        profileService.updatePassword(StpUtil.getLoginIdAsLong(), request);
        return R.ok();
    }

    @Log(title = "修改头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public R<Long> updateAvatar(@RequestParam("file") MultipartFile file) {
        try {
            return R.ok(profileService.updateAvatar(StpUtil.getLoginIdAsLong(), file));
        } catch (IllegalArgumentException | IOException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/avatar")
    public void avatar(HttpServletResponse response) throws IOException {
        Long fileId = profileService.getAvatarFileId(StpUtil.getLoginIdAsLong());
        if (fileId == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        fileService.downloadById(fileId, response);
    }
}
