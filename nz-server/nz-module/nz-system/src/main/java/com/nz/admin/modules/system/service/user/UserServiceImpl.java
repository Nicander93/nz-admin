package com.nz.admin.modules.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.framework.datascope.DataScope;
import com.nz.admin.framework.datascope.DataScopeType;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserPostDO;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserPostMapper;
import com.nz.admin.modules.system.entity.query.user.UserQuery;
import com.nz.admin.modules.system.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户这块的服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserPostMapper userPostMapper;
    @Autowired
    private ConfigService configService;

    /**
     * 按分页条件查用户列表。
     */
    @DataScope(value = {DataScopeType.ALL, DataScopeType.DEPT, DataScopeType.SELF}, deptAlias = "dept_id", userAlias = "id")
    @Override
    public Page<UserDO> listPage(UserQuery query) {
        return userMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 拿用户详情。
     */
    @Override
    public UserDO getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 按用户名查用户。
     */
    @Override
    public UserDO getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 新增一条用户记录。
     */
    @Override
    public void save(UserDO user) {
        userMapper.insert(user);
    }

    /**
     * 按 id 更新用户。
     */
    @Override
    public void updateById(UserDO user) {
        userMapper.updateById(user);
    }

    /**
     * 按 id 删掉用户。
     */
    @Override
    @Transactional
    public void removeById(Long id) {
        userPostMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    @Override
    public List<Long> getPostIdsByUserId(Long userId) {
        return userPostMapper.selectByUserId(userId).stream().map(UserPostDO::getPostId).toList();
    }

    @Override
    @Transactional
    public void assignUserPosts(Long userId, List<Long> postIds) {
        userPostMapper.deleteByUserId(userId);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        for (Long postId : postIds) {
            UserPostDO row = new UserPostDO();
            row.setUserId(userId);
            row.setPostId(postId);
            userPostMapper.insert(row);
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long userId) {
        String raw = configService.getConfigValue("sys.user.initPassword");
        if (StrUtil.isBlank(raw)) {
            raw = "123456";
        }
        String hashed = BCrypt.hashpw(raw);
        userMapper.update(null, new LambdaUpdateWrapper<UserDO>()
                .set(UserDO::getPassword, hashed)
                .eq(UserDO::getId, userId));
    }

    /**
     * 查当前用户总数。
     */
    @Override
    public long count() {
        return userMapper.selectCount(null);
    }
}
