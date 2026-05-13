package com.nz.admin.framework.mybatis.core.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nz.admin.common.core.PageQuery;
import com.nz.admin.common.core.PageResult;

/**
 * MyBatis 扩展 Mapper 基类。
 */
public interface BaseMapperX<T> extends BaseMapper<T> {

    default PageResult<T> selectPage(PageQuery pageQuery, Wrapper<T> queryWrapper) {
        return PageResult.of(selectPage(pageQuery.toPage(), queryWrapper));
    }

    default PageResult<T> toPageResult(IPage<T> page) {
        return PageResult.of(page);
    }
}
