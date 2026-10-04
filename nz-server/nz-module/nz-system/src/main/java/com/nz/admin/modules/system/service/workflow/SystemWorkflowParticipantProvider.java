package com.nz.admin.modules.system.service.workflow;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.module.NzWorkflowParticipantProvider;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 租户拦截器覆盖全部查询，不向设计器暴露用户实体或个人联系方式。 */
@Component
public class SystemWorkflowParticipantProvider implements NzWorkflowParticipantProvider {
    private final UserMapper users;
    private final RoleMapper roles;
    public SystemWorkflowParticipantProvider(UserMapper users, RoleMapper roles) {
        this.users = users;
        this.roles = roles;
    }
    @Override
    public Selection select(String type, String code, String name, int page, int size) {
        if ("role".equals(type)) {
            var result = roles.selectPage(new Page<>(page, size), new LambdaQueryWrapper<RoleDO>()
                    .eq(RoleDO::getStatus, 0).like(StrUtil.isNotBlank(code), RoleDO::getRoleKey, code)
                    .like(StrUtil.isNotBlank(name), RoleDO::getName, name).orderByAsc(RoleDO::getId));
            return new Selection(result.getRecords().stream().map(this::role).toList(), result.getTotal());
        }
        var result = users.selectPage(new Page<>(page, size), new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, 0).like(StrUtil.isNotBlank(code), UserDO::getUsername, code)
                .like(StrUtil.isNotBlank(name), UserDO::getNickname, name).orderByAsc(UserDO::getId));
        return new Selection(result.getRecords().stream().map(this::user).toList(), result.getTotal());
    }
    @Override
    public List<Participant> feedback(List<String> ids) {
        var result = new ArrayList<Participant>();
        var userIds = ids.stream().filter(v -> v.matches("(?:user:)?[1-9][0-9]*"))
                .map(v -> Long.valueOf(v.replace("user:", ""))).toList();
        var roleKeys = ids.stream().filter(v -> v.matches("role:[A-Za-z][A-Za-z0-9_-]*"))
                .map(v -> v.substring(5)).toList();
        users.selectActiveByIds(userIds).forEach(v -> result.add(user(v)));
        roles.selectActiveByKeys(roleKeys).forEach(v -> result.add(role(v)));
        return result;
    }
    private Participant user(UserDO value) {
        return new Participant("user:" + value.getId(), value.getUsername(),
                value.getNickname(), "用户", null);
    }
    private Participant role(RoleDO value) {
        return new Participant("role:" + value.getRoleKey(), value.getRoleKey(), value.getName(), "角色", null);
    }
}
