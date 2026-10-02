package com.nz.admin.modules.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.datascope.core.DataScopeContext;
import com.nz.admin.framework.datascope.core.DataScopeResolver;
import com.nz.admin.framework.encryption.core.FieldCipher;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserPostDO;
import com.nz.admin.modules.system.entity.query.user.UserQuery;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserPostMapper;
import com.nz.admin.modules.system.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private FieldCipher fieldCipher;
    @Autowired
    private DataScopeResolver dataScopeResolver;
    @Autowired
    private DeptMapper deptMapper;


    /**
     * 按分页条件查用户列表。
     */
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

    @Override
    public UserDO getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        String normalizedPhone = phone.trim();
        String phoneHash = DigestUtil.sha256Hex(normalizedPhone);
        List<UserDO> matches = userMapper.selectByPhoneHash(phoneHash);
        if (matches.size() > 1) {
            throw new BusinessException("同一租户存在重复手机号，请先修复用户数据");
        }
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        for (UserDO user : userMapper.selectList(null)) {
            if (normalizedPhone.equals(user.getPhone())) {
                userMapper.updateById(new UserDO().setId(user.getId()).setPhoneHash(phoneHash));
                return user;
            }
        }
        return null;
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
    @Transactional
    public void save(UserDO user) {
        validateDepartment(user.getDeptId());
        checkTenantAccountLimit();
        preparePhoneHash(user);
        userMapper.insert(user);
    }

    /**
     * 按 id 更新用户。
     */
    @Override
    public void updateById(UserDO user) {
        requireUser(user.getId());
        if (user.getDeptId() != null) {
            validateDepartment(user.getDeptId());
        }
        preparePhoneHash(user);
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException("用户不存在或无权修改");
        }
    }

    /**
     * 按 id 删掉用户。
     */
    @Override
    @Transactional
    public void removeById(Long id) {
        if (userMapper.deleteById(id) != 1) {
            throw new BusinessException("用户不存在或无权删除");
        }
        userPostMapper.deleteByUserId(id);
    }

    @Override
    public List<Long> getPostIdsByUserId(Long userId) {
        requireUser(userId);
        return userPostMapper.selectByUserId(userId).stream().map(UserPostDO::getPostId).toList();
    }

    @Override
    @Transactional
    public void assignUserPosts(Long userId, List<Long> postIds) {
        requireUser(userId);
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
        requireUser(userId);
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

    @Override
    public List<UserDO> listEnabledUsers(Collection<Long> userIds) {
        return userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, 0)
                .in(userIds != null && !userIds.isEmpty(), UserDO::getId, userIds)
                .orderByAsc(UserDO::getId));
    }

    /**
     * 用活动密钥重写当前租户下的用户联系方式。
     */
    @Override
    @Transactional
    public int reEncryptContacts() {
        if ("disabled".equals(fieldCipher.activeKeyId())) {
            throw new BusinessException("字段加密未启用，不能执行联系方式重加密");
        }
        List<UserDO> users = userMapper.selectList(null);
        for (UserDO user : users) {
            UserDO update = new UserDO()
                    .setId(user.getId())
                    .setEmail(user.getEmail())
                    .setPhone(user.getPhone());
            preparePhoneHash(update);
            userMapper.updateById(update);
        }
        return users.size();
    }

    private void preparePhoneHash(UserDO user) {
        if (user != null && StrUtil.isNotBlank(user.getPhone())) {
            user.setPhone(user.getPhone().trim());
            user.setPhoneHash(DigestUtil.sha256Hex(user.getPhone()));
        }
    }

    private void checkTenantAccountLimit() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) {
            return;
        }
        TenantDO tenant = tenantMapper.selectOne(new LambdaQueryWrapper<TenantDO>().eq(TenantDO::getId, tenantId).last("FOR UPDATE"));
        if (tenant == null || tenant.getAccountCount() == null || tenant.getAccountCount() <= 0) {
            return;
        }
        Long currentCount = DataScopeContext.withoutFilter(() -> userMapper.selectCount(null));
        if (currentCount != null && currentCount >= tenant.getAccountCount()) {
            throw new BusinessException("租户账号数量已达到套餐上限");
        }
    }

    private void requireUser(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("用户不存在或无权访问");
        }
    }

    private void validateDepartment(Long deptId) {
        if (deptId != null && deptMapper.selectById(deptId) == null) {
            throw new BusinessException("部门不存在");
        }
        if (DataScopeContext.isIgnored()) {
            return;
        }
        var scope = DataScopeContext.withoutFilter(dataScopeResolver::resolve);
        if (!scope.all() && (deptId == null || !scope.deptIds().contains(deptId))) {
            throw new BusinessException("不能将用户归属到该部门");
        }
    }

    @Override
    @Transactional
    public void createWithPosts(UserDO user, List<Long> postIds) {
        user.setId(null);
        user.setTenantId(TenantContextHolder.getTenantIdOrNull());
        save(user);
        assignUserPosts(user.getId(), postIds);
    }

    @Override
    @Transactional
    public void updateWithPosts(UserDO user, List<Long> postIds) {
        user.setTenantId(null);
        updateById(user);
        if (postIds != null) {
            assignUserPosts(user.getId(), postIds);
        }
    }

}
