package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysFileDO;
import com.nz.admin.modules.system.entity.query.SysFileQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传 Mapper。
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFileDO> {

    /**
     * 按条件分页查询文件记录。
     */
    default Page<SysFileDO> selectPageByCondition(Page<SysFileDO> page, SysFileQuery query) {
        LambdaQueryWrapper<SysFileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getOriginalName()), 
                        SysFileDO::getOriginalName, query.getOriginalName())
               .eq(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getFileExt()), 
                    SysFileDO::getFileExt, query.getFileExt())
               .orderByDesc(SysFileDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
