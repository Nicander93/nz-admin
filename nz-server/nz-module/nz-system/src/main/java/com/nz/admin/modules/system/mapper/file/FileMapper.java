package com.nz.admin.modules.system.mapper.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.file.FileDO;
import com.nz.admin.modules.system.entity.query.file.FileQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件上传 Mapper。
 */
@Mapper
public interface FileMapper extends BaseMapper<FileDO> {

    /**
     * 按条件分页查询文件记录。
     */
    default Page<FileDO> selectPageByCondition(Page<FileDO> page, FileQuery query) {
        LambdaQueryWrapper<FileDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getOriginalName()), 
                        FileDO::getOriginalName, query.getOriginalName())
               .eq(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(query.getFileExt()), 
                    FileDO::getFileExt, query.getFileExt())
               .orderByDesc(FileDO::getCreateTime);
        return selectPage(page, wrapper);
    }
}
