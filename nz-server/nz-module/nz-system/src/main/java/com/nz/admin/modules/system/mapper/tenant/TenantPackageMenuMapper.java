package com.nz.admin.modules.system.mapper.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TenantPackageMenuMapper extends BaseMapper<TenantPackageMenuDO> {

    default List<TenantPackageMenuDO> selectByPackageId(Long packageId) {
        return selectList(new LambdaQueryWrapper<TenantPackageMenuDO>()
                .eq(TenantPackageMenuDO::getPackageId, packageId));
    }

    default void deleteByPackageId(Long packageId) {
        delete(new LambdaQueryWrapper<TenantPackageMenuDO>()
                .eq(TenantPackageMenuDO::getPackageId, packageId));
    }
}
