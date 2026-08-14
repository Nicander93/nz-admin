package com.nz.admin.modules.system.service.profile;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.file.FileSecurityValidator;
import com.nz.admin.modules.system.entity.dataobject.dept.PostDO;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dto.profile.PasswordUpdateRequest;
import com.nz.admin.modules.system.entity.dto.profile.ProfileUpdateRequest;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.service.dept.PostService;
import com.nz.admin.modules.system.service.file.FileService;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.role.RoleService;
import com.nz.admin.modules.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceImplTest {

    private UserService userService;
    private UserMapper userMapper;
    private PermissionService permissionService;
    private RoleService roleService;
    private PostService postService;
    private FileService fileService;
    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userMapper = mock(UserMapper.class);
        permissionService = mock(PermissionService.class);
        roleService = mock(RoleService.class);
        postService = mock(PostService.class);
        fileService = mock(FileService.class);
        service = new ProfileServiceImpl(
                userService, userMapper, permissionService, roleService,
                postService, fileService, mock(FileSecurityValidator.class));
    }

    @Test
    void returnsCurrentUsersRoleAndPostGroups() {
        when(userService.getById(7L)).thenReturn(new UserDO()
                .setId(7L).setUsername("alice").setNickname("Alice").setGender("2"));
        when(permissionService.getRoleIdsByUserId(7L)).thenReturn(List.of(2L));
        when(userService.getPostIdsByUserId(7L)).thenReturn(List.of(3L));
        when(roleService.listAll()).thenReturn(List.of(new RoleDO().setId(2L).setName("运维")));
        when(postService.listAll()).thenReturn(List.of(new PostDO().setId(3L).setPostName("工程师")));

        var profile = service.getProfile(7L);

        assertThat(profile.username()).isEqualTo("alice");
        assertThat(profile.roleGroup()).isEqualTo("运维");
        assertThat(profile.postGroup()).isEqualTo("工程师");
    }

    @Test
    void rejectsDuplicateContactsInsideCurrentTenant() {
        when(userService.getById(7L)).thenReturn(new UserDO().setId(7L));
        when(userMapper.selectList(null)).thenReturn(List.of(
                new UserDO().setId(8L).setEmail("used@example.com").setPhone("13800138000")));

        assertThatThrownBy(() -> service.updateProfile(7L, new ProfileUpdateRequest(
                "Alice", "used@example.com", "", "2")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被其他用户使用");
        verify(userService, never()).updateById(any());
    }

    @Test
    void rejectsWrongOldPassword() {
        when(userService.getById(7L)).thenReturn(new UserDO()
                .setId(7L).setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("old-password")));

        assertThatThrownBy(() -> service.updatePassword(
                7L, new PasswordUpdateRequest("wrong", "new-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("旧密码错误");
        verify(userService, never()).updateById(any());
    }

    @Test
    void replacesAvatarAndCleansPreviousManagedFile() throws Exception {
        when(userService.getById(7L)).thenReturn(new UserDO()
                .setId(7L).setAvatarFileId(20L));
        when(fileService.upload(any(), eq(7L))).thenReturn(new FileDO().setId(21L));
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        assertThat(service.updateAvatar(7L, file)).isEqualTo(21L);

        verify(userService).updateById(argThat(user ->
                Long.valueOf(7L).equals(user.getId())
                        && Long.valueOf(21L).equals(user.getAvatarFileId())));
        verify(fileService).removeById(20L);
    }
}
