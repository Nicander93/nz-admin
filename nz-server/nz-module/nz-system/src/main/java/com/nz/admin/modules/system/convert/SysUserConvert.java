package com.nz.admin.modules.system.convert;

import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.entity.vo.SysUserRespVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户相关转换。
 */
@org.mapstruct.Mapper
public interface SysUserConvert {

    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);

    SysUserRespVO convert(SysUserDO user);

    List<SysUserRespVO> convertList(List<SysUserDO> list);

    default Page<SysUserRespVO> convertPage(Page<SysUserDO> page) {
        if (page == null) {
            return null;
        }
        Page<SysUserRespVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(convertList(page.getRecords()));
        return result;
    }
}

