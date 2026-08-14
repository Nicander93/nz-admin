package com.nz.admin.modules.system.service.profile;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileSecurityValidator;
import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dto.profile.PasswordUpdateRequest;
import com.nz.admin.modules.system.entity.dto.profile.ProfileUpdateRequest;
import com.nz.admin.modules.system.entity.vo.profile.ProfileVO;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.service.dept.PostService;
import com.nz.admin.modules.system.service.file.FileService;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.role.RoleService;
import com.nz.admin.modules.system.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 当前登录用户个人资料服务实现。 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final PostService postService;
    private final FileService fileService;
    private final FileSecurityValidator fileSecurityValidator;

    public ProfileServiceImpl(UserService userService,
                              UserMapper userMapper,
                              PermissionService permissionService,
                              RoleService roleService,
                              PostService postService,
                              FileService fileService,
                              FileSecurityValidator fileSecurityValidator) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.roleService = roleService;
        this.postService = postService;
        this.fileService = fileService;
        this.fileSecurityValidator = fileSecurityValidator;
    }

    @Override
    public ProfileVO getProfile(Long userId) {
        UserDO user = requireUser(userId);
        Set<Long> roleIds = new HashSet<>(permissionService.getRoleIdsByUserId(userId));
        Set<Long> postIds = new HashSet<>(userService.getPostIdsByUserId(userId));
        String roleGroup = joinRoleNames(roleIds);
        String postGroup = joinPostNames(postIds);
        return new ProfileVO(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getEmail(), user.getPhone(), user.getGender(),
                user.getAvatarFileId(), roleGroup, postGroup, user.getCreateTime());
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        requireUser(userId);
        String email = StrUtil.trim(request.email());
        String phone = StrUtil.trim(request.phone());
        checkContactsUnique(userId, email, phone);
        userService.updateById(new UserDO()
                .setId(userId)
                .setNickname(request.nickname().trim())
                .setEmail(StrUtil.isBlank(email) ? null : email)
                .setPhone(StrUtil.isBlank(phone) ? null : phone)
                .setGender(request.gender()));
        clearBlankContacts(userId, email, phone);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserDO user = requireUser(userId);
        if (!BCrypt.checkpw(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        if (BCrypt.checkpw(request.newPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        userService.updateById(new UserDO()
                .setId(userId)
                .setPassword(BCrypt.hashpw(request.newPassword())));
    }

    @Override
    public Long updateAvatar(Long userId, MultipartFile file) throws IOException {
        UserDO user = requireUser(userId);
        validateAvatar(file);
        FileDO uploaded = fileService.upload(file, userId);
        try {
            userService.updateById(new UserDO()
                    .setId(userId)
                    .setAvatarFileId(uploaded.getId()));
        } catch (RuntimeException e) {
            fileService.removeById(uploaded.getId());
            throw e;
        }
        if (user.getAvatarFileId() != null && !user.getAvatarFileId().equals(uploaded.getId())) {
            try {
                fileService.removeById(user.getAvatarFileId());
            } catch (RuntimeException e) {
                log.warn("旧头像文件 {} 清理失败", user.getAvatarFileId(), e);
            }
        }
        return uploaded.getId();
    }

    @Override
    public Long getAvatarFileId(Long userId) {
        return requireUser(userId).getAvatarFileId();
    }

    private UserDO requireUser(Long userId) {
        UserDO user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        return user;
    }

    private void checkContactsUnique(Long userId, String email, String phone) {
        for (UserDO candidate : userMapper.selectList(null)) {
            if (Objects.equals(userId, candidate.getId())) {
                continue;
            }
            if (StrUtil.isNotBlank(email) && StrUtil.equalsIgnoreCase(email, candidate.getEmail())) {
                throw new BusinessException("邮箱已被其他用户使用");
            }
            if (StrUtil.isNotBlank(phone) && StrUtil.equals(phone, candidate.getPhone())) {
                throw new BusinessException("手机号已被其他用户使用");
            }
        }
    }

    private void clearBlankContacts(Long userId, String email, String phone) {
        if (StrUtil.isNotBlank(email) && StrUtil.isNotBlank(phone)) {
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                .set(StrUtil.isBlank(email), UserDO::getEmail, null)
                .set(StrUtil.isBlank(phone), UserDO::getPhone, null)
                .set(StrUtil.isBlank(phone), UserDO::getPhoneHash, null)
                .eq(UserDO::getId, userId));
    }

    private String joinRoleNames(Set<Long> roleIds) {
        return roleService.listAll().stream()
                .filter(role -> roleIds.contains(role.getId()))
                .map(RoleDO::getName)
                .filter(StrUtil::isNotBlank)
                .sorted()
                .reduce((left, right) -> left + "、" + right)
                .orElse("未分配");
    }

    private String joinPostNames(Set<Long> postIds) {
        return postService.listAll().stream()
                .filter(post -> postIds.contains(post.getId()))
                .map(PostDO::getPostName)
                .filter(StrUtil::isNotBlank)
                .sorted()
                .reduce((left, right) -> left + "、" + right)
                .orElse("未分配");
    }

    private void validateAvatar(MultipartFile file) {
        fileSecurityValidator.validateUpload(file);
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("头像必须是图片文件");
        }
    }
}
