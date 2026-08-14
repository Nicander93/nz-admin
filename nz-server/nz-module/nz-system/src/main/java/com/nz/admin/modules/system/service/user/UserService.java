package com.nz.admin.modules.system.service.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.query.user.UserQuery;

import java.util.Collection;
import java.util.List;

public interface UserService {

    Page<UserDO> listPage(UserQuery query);

    UserDO getById(Long id);
    UserDO getByPhone(String phone);


    UserDO getByUsername(String username);

    void save(UserDO user);

    void updateById(UserDO user);

    void removeById(Long id);

    long count();

    List<UserDO> listEnabledUsers(Collection<Long> userIds);

    List<Long> getPostIdsByUserId(Long userId);

    void assignUserPosts(Long userId, List<Long> postIds);

    void resetPassword(Long userId);

    int reEncryptContacts();
}
