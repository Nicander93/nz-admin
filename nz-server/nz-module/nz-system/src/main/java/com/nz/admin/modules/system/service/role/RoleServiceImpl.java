package com.nz.admin.modules.system.service.role;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDeptDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.query.role.RoleQuery;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.mapper.role.RoleDeptMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import com.nz.admin.modules.system.service.role.RoleService;
import com.nz.admin.modules.system.service.tenant.TenantPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;

/**
 * 角色这块的服务实现。
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleDeptMapper roleDeptMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private TenantPackageService tenantPackageService;
    @Autowired
    private TenantProperties tenantProperties;

    /**
     * 按分页条件查角色列表。
     */
    @Override
    public Page<RoleDO> listPage(RoleQuery query) {
        var page = roleMapper.selectPageByCondition(query.toPage(), query);
        page.getRecords().forEach(this::loadDepartments);
        return page;
    }

    /**
     * 把所有角色查出来。
     */
    @Override
    public List<RoleDO> listAll() {
        return roleMapper.selectList(null);
    }

    /**
     * 按 id 拿角色详情。
     */
    @Override
    public RoleDO getById(Long id) {
        var role = roleMapper.selectById(id);
        if (role != null) {
            loadDepartments(role);
        }
        return role;
    }

    /**
     * 新增一条角色记录。
     */
    @Override
    @Transactional
    public void save(RoleDO role) {
        validateScope(role);
        roleMapper.insert(role);
        saveDepartments(role);
    }

    /**
     * 按 id 更新角色。
     */
    @Override
    @Transactional
    public void updateById(RoleDO role) {
        RoleDO current = getById(role.getId());
        if (current == null) {
            throw new BusinessException("角色不存在");
        }
        if (role.getDataScope() == null) {
            role.setDataScope(current.getDataScope());
        }
        if (role.getDataScope() != null && role.getDataScope() == 2 && role.getDeptIds() == null) {
            role.setDeptIds(current.getDeptIds());
        }
        validateScope(role);
        roleMapper.updateById(role);
        saveDepartments(role);
    }

    /**
     * 删除角色，并把角色菜单关联一并清掉。
     */
    @Override
    @Transactional
    public void removeById(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
        roleDeptMapper.deleteByRoleId(id);
    }

    /**
     * 按角色 id 拿菜单 id 列表。
     */
    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectByRoleId(roleId).stream()
                .map(RoleMenuDO::getMenuId).toList();
    }

    /**
     * 给角色重新分配菜单。
     */
    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 这里走覆盖式分配：先清掉旧关联，再写入新关联。
        checkMenusWithinTenantPackage(menuIds);
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            RoleMenuDO rm = new RoleMenuDO();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    private void checkMenusWithinTenantPackage(List<Long> menuIds) {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null || tenantProperties.getDefaultTenantId().equals(tenantId)) {
            return;
        }
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("当前租户不存在");
        }
        var allowedMenuIds = tenantPackageService.getMenuIds(tenant.getPackageId());
        if (menuIds != null && !allowedMenuIds.containsAll(menuIds)) {
            throw new BusinessException("角色菜单超出租户套餐范围");
        }
    }

    private void loadDepartments(RoleDO role) {
        role.setDeptIds(roleDeptMapper.selectByRoleId(role.getId()).stream().map(row -> row.getDeptId()).toList());
    }

    private void validateScope(RoleDO role) {
        if (role.getDataScope() == null) {
            role.setDataScope(5);
        }
        if (role.getDataScope() < 1 || role.getDataScope() > 5) {
            throw new BusinessException("数据范围不合法");
        }
        if (role.getDataScope() == 2) {
            if (role.getDeptIds() == null || role.getDeptIds().isEmpty()) {
                throw new BusinessException("请选择自定义部门");
            }
            for (Long id : role.getDeptIds()) {
                if (id == null || deptMapper.selectById(id) == null) {
                    throw new BusinessException("部门不存在");
                }
            }
        }
    }

    private void saveDepartments(RoleDO role) {
        roleDeptMapper.deleteByRoleId(role.getId());
        if (role.getDataScope() != 2) {
            return;
        }
        for (Long id : new LinkedHashSet<>(role.getDeptIds())) {
            var relation = new RoleDeptDO();
            relation.setRoleId(role.getId());
            relation.setDeptId(id);
            roleDeptMapper.insert(relation);
        }
    }

}
