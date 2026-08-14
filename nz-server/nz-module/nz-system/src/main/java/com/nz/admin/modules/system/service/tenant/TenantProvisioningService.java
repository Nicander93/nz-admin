package com.nz.admin.modules.system.service.tenant;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.config.ConfigDO;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.mapper.config.ConfigMapper;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantPackageMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantPackageMenuMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 创建租户基础数据，并把套餐菜单同步到租户管理员角色。
 */
@Service
public class TenantProvisioningService {

    private final TenantPackageMapper tenantPackageMapper;
    private final TenantPackageMenuMapper tenantPackageMenuMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ConfigMapper configMapper;

    public TenantProvisioningService(TenantPackageMapper tenantPackageMapper,
                                     TenantPackageMenuMapper tenantPackageMenuMapper,
                                     DeptMapper deptMapper,
                                     RoleMapper roleMapper,
                                     RoleMenuMapper roleMenuMapper,
                                     UserMapper userMapper,
                                     UserRoleMapper userRoleMapper,
                                     ConfigMapper configMapper) {
        this.tenantPackageMapper = tenantPackageMapper;
        this.tenantPackageMenuMapper = tenantPackageMenuMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.configMapper = configMapper;
    }

    public void provision(TenantDO tenant, TenantCreateRequest request) {
        TenantPackageDO tenantPackage = getEnabledPackage(tenant.getPackageId());
        Set<Long> menuIds = getPackageMenuIds(tenantPackage.getId());

        TenantContextHolder.runWithTenantId(tenant.getId(), () -> {
            DeptDO dept = new DeptDO();
            dept.setParentId(0L);
            dept.setName(tenant.getTenantName());
            dept.setSort(0);
            dept.setStatus(0);
            deptMapper.insert(dept);

            RoleDO role = new RoleDO();
            role.setName("租户管理员");
            role.setRoleKey("admin");
            role.setSort(0);
            role.setStatus(0);
            role.setDataScope(1);
            role.setRemark("租户初始化时自动创建");
            roleMapper.insert(role);
            replaceRoleMenus(role.getId(), menuIds);

            UserDO user = new UserDO();
            user.setTenantId(tenant.getId());
            user.setDeptId(dept.getId());
            user.setUsername(request.getAdminUsername());
            user.setPassword(BCrypt.hashpw(request.getAdminPassword()));
            user.setNickname(request.getContactUser() == null ? "租户管理员" : request.getContactUser());
            user.setPhone(request.getContactPhone());
            user.setStatus(0);
            userMapper.insert(user);

            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);

            saveConfig("主框架页签", "sys.index.tabs", "true", "是否开启页签风格");
            saveConfig("默认密码", "sys.user.initPassword", "123456", "新用户默认密码");
        });
    }

    public void applyPackageMenus(Long tenantId, Long packageId) {
        getEnabledPackage(packageId);
        Set<Long> menuIds = getPackageMenuIds(packageId);
        TenantContextHolder.runWithTenantId(tenantId, () -> {
            RoleDO adminRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                    .eq(RoleDO::getRoleKey, "admin"));
            if (adminRole != null) {
                replaceRoleMenus(adminRole.getId(), menuIds);
            }
        });
    }

    private TenantPackageDO getEnabledPackage(Long packageId) {
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(packageId);
        if (tenantPackage == null) {
            throw new BusinessException("租户套餐不存在");
        }
        if (tenantPackage.getStatus() != null && tenantPackage.getStatus() != 0) {
            throw new BusinessException("租户套餐已停用");
        }
        return tenantPackage;
    }

    private Set<Long> getPackageMenuIds(Long packageId) {
        Set<Long> menuIds = new LinkedHashSet<>();
        tenantPackageMenuMapper.selectByPackageId(packageId)
                .forEach(row -> menuIds.add(row.getMenuId()));
        return menuIds;
    }

    private void replaceRoleMenus(Long roleId, Set<Long> menuIds) {
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            RoleMenuDO relation = new RoleMenuDO();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private void saveConfig(String name, String key, String value, String remark) {
        ConfigDO config = new ConfigDO();
        config.setConfigName(name);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(1);
        config.setStatus(0);
        config.setRemark(remark);
        configMapper.insert(config);
    }
}
