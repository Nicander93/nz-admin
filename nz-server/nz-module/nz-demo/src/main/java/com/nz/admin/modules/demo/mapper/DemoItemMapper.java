package com.nz.admin.modules.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 示例条目数据访问。
 */
@Mapper
public interface DemoItemMapper extends BaseMapper<DemoItemDO> {
}
