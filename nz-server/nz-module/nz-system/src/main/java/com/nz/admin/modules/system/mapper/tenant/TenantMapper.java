package com.nz.admin.modules.system.mapper.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<TenantDO> {

    default TenantDO selectByCode(String tenantCode) {
        return selectOne(new LambdaQueryWrapper<TenantDO>()
                .eq(TenantDO::getTenantCode, tenantCode));
    }
}
