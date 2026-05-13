package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.entity.query.user.UserQuery;
import com.nz.admin.modules.system.service.user.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class UserServiceImplTest extends BaseDbUnitTest {

    @Resource
    private UserServiceImpl userService;
    @Resource
    private UserMapper userMapper;

    @Test
    void testListPage() {
        UserDO user1 = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("admin_test_1")
                .setNickname("管理员1");
        userMapper.insert(user1);

        UserDO user2 = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("guest_test_1")
                .setNickname("访客1");
        userMapper.insert(user2);

        UserQuery query = new UserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin_test_1");

        Page<UserDO> result = userService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("admin_test_1", result.getRecords().get(0).getUsername());
    }

    @Test
    void testGetById() {
        UserDO user = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("admin_get_by_id");
        userMapper.insert(user);

        UserDO result = userService.getById(user.getId());

        assertNotNull(result);
        assertEquals("admin_get_by_id", result.getUsername());
    }

    @Test
    void testGetByUsername() {
        UserDO user = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("admin_get_by_username");
        userMapper.insert(user);

        UserDO result = userService.getByUsername("admin_get_by_username");

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void testGetByUsername_notFound() {
        UserDO result = userService.getByUsername("nonexistent_user");
        assertNull(result);
    }

    @Test
    void testSave() {
        UserDO user = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("new_user_for_save");

        userService.save(user);

        assertNotNull(user.getId());
        assertNotNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testUpdateById() {
        UserDO user = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("user_for_update")
                .setNickname("原昵称");
        userMapper.insert(user);

        UserDO update = new UserDO()
                .setId(user.getId())
                .setNickname("新昵称");
        userService.updateById(update);

        UserDO dbUser = userMapper.selectById(user.getId());
        assertNotNull(dbUser);
        assertEquals("新昵称", dbUser.getNickname());
    }

    @Test
    void testRemoveById() {
        UserDO user = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("user_for_remove");
        userMapper.insert(user);

        userService.removeById(user.getId());

        assertNull(userMapper.selectById(user.getId()));
    }

    @Test
    void testCount() {
        UserDO user1 = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("count_user_1");
        userMapper.insert(user1);

        UserDO user2 = randomPojo(UserDO.class)
                .setId(null)
                .setUsername("count_user_2");
        userMapper.insert(user2);

        long count = userService.count();

        assertEquals(2L, count);
    }
}
