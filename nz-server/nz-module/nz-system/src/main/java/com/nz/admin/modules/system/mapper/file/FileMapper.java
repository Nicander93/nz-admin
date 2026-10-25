package com.nz.admin.modules.system.mapper.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.query.file.FileQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.nz.admin.framework.tenant.core.TenantContextHolder;

/**
 * 文件上传 Mapper。
 */
@Mapper
public interface FileMapper extends BaseMapper<FileDO> {

    @Select("SELECT * FROM sys_file WHERE id=#{id} AND tenant_id=#{tenantId} FOR UPDATE")
    FileDO selectForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM sys_file WHERE id=#{id} AND tenant_id=#{tenantId} FOR UPDATE SKIP LOCKED")
    FileDO selectForCleanup(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 按条件分页查询文件记录。
     */
    default Page<FileDO> selectPageByCondition(Page<FileDO> page, FileQuery query) {
        LambdaQueryWrapper<FileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDO::getTenantId, (TenantContextHolder.getTenantIdOrNull() == null ? 1L : TenantContextHolder.getTenantIdOrNull()))
               .ne(FileDO::getLifecycleStatus, "UPLOADING")
               .like(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getOriginalName()),
                        FileDO::getOriginalName, query.getOriginalName())
               .eq(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getFileExt()),
                    FileDO::getFileExt, query.getFileExt())
               .orderByDesc(FileDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
