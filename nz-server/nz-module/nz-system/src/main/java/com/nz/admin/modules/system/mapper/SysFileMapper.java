package com.nz.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysFile;
import com.nz.admin.modules.system.query.SysFileQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传 Mapper。
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 按条件分页查询文件记录。
     */
    default Page<SysFile> selectPageByCondition(Page<SysFile> page, SysFileQuery query) {
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getOriginalName()), 
                        SysFile::getOriginalName, query.getOriginalName())
               .eq(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getFileExt()), 
                    SysFile::getFileExt, query.getFileExt())
               .orderByDesc(SysFile::getCreateTime);
        return selectPage(page, wrapper);
    }
}
