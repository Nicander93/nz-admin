package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.mapper.SysUserMapper;
import com.nz.admin.modules.system.entity.query.SysUserQuery;
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
        SysUserDO user1 = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("admin_test_1")
                .setNickname("管理员1");
        userMapper.insert(user1);

        SysUserDO user2 = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("guest_test_1")
                .setNickname("访客1");
        userMapper.insert(user2);

        SysUserQuery query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin_test_1");

        Page<SysUserDO> result = userService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("admin_test_1", result.getRecords().get(0).getUsername());
    }

    @Test
    void testGetById() {
        SysUserDO user = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("admin_get_by_id");
        userMapper.insert(user);

        SysUserDO result = userService.getById(user.getId());

        assertNotNull(result);
        assertEquals("admin_get_by_id", result.getUsername());
    }

    @Test
    void testGetByUsername() {
        SysUserDO user = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("admin_get_by_username");
        userMapper.insert(user);

        SysUserDO result = userService.getByUsername("admin_get_by_username");

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void testGetByUsername_notFound() {
        SysUserDO result = userService.getByUsername("nonexistent_user");
        assertNull(result);
    }

    @Test
    void testSave() {
        SysUserDO user = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("new_user_for_save");

        userService.save(user);

        assertNotNull(user.getId());
        assertNotNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testUpdateById() {
        SysUserDO user = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("user_for_update")
                .setNickname("原昵称");
        userMapper.insert(user);

        SysUserDO update = new SysUserDO()
                .setId(user.getId())
                .setNickname("新昵称");
        userService.updateById(update);

        SysUserDO dbUser = userMapper.selectById(user.getId());
        assertNotNull(dbUser);
        assertEquals("新昵称", dbUser.getNickname());
    }

    @Test
    void testRemoveById() {
        SysUserDO user = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("user_for_remove");
        userMapper.insert(user);

        userService.removeById(user.getId());

        assertNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testCount() {
        SysUserDO user1 = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("count_user_1");
        userMapper.insert(user1);

        SysUserDO user2 = randomPojo(SysUserDO.class)
                .setId(null)
                .setUsername("count_user_2");
        userMapper.insert(user2);

        long count = userService.count();

        assertEquals(2L, count);
    }
}
