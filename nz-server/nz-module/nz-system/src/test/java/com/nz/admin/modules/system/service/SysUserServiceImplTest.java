package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.mapper.SysUserMapper;
import com.nz.admin.modules.system.query.SysUserQuery;
import com.nz.admin.modules.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class SysUserServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysUserServiceImpl userService;
    @Resource
    private SysUserMapper userMapper;

    @Test
    void testListPage() {
        SysUser user1 = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("admin_test_1")
                .setNickname("管理员1");
        userMapper.insert(user1);

        SysUser user2 = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("guest_test_1")
                .setNickname("访客1");
        userMapper.insert(user2);

        SysUserQuery query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin_test_1");

        Page<SysUser> result = userService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("admin_test_1", result.getRecords().get(0).getUsername());
    }

    @Test
    void testGetById() {
        SysUser user = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("admin_get_by_id");
        userMapper.insert(user);

        SysUser result = userService.getById(user.getId());

        assertNotNull(result);
        assertEquals("admin_get_by_id", result.getUsername());
    }

    @Test
    void testGetByUsername() {
        SysUser user = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("admin_get_by_username");
        userMapper.insert(user);

        SysUser result = userService.getByUsername("admin_get_by_username");

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void testGetByUsername_notFound() {
        SysUser result = userService.getByUsername("nonexistent_user");
        assertNull(result);
    }

    @Test
    void testSave() {
        SysUser user = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("new_user_for_save");

        userService.save(user);

        assertNotNull(user.getId());
        assertNotNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testUpdateById() {
        SysUser user = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("user_for_update")
                .setNickname("原昵称");
        userMapper.insert(user);

        SysUser update = new SysUser()
                .setId(user.getId())
                .setNickname("新昵称");
        userService.updateById(update);

        SysUser dbUser = userMapper.selectById(user.getId());
        assertNotNull(dbUser);
        assertEquals("新昵称", dbUser.getNickname());
    }

    @Test
    void testRemoveById() {
        SysUser user = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("user_for_remove");
        userMapper.insert(user);

        userService.removeById(user.getId());

        assertNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testCount() {
        SysUser user1 = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("count_user_1");
        userMapper.insert(user1);

        SysUser user2 = randomPojo(SysUser.class)
                .setId(null)
                .setUsername("count_user_2");
        userMapper.insert(user2);

        long count = userService.count();

        assertEquals(2L, count);
    }
}
