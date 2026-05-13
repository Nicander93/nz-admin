package com.nz.admin.modules.system.convert.user;

import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.vo.user.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户相关转换。
 */
@org.mapstruct.Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserVO toVO(UserDO user);

    List<UserVO> toVOList(List<UserDO> list);

    default Page<UserVO> toVOPage(Page<UserDO> page) {
        if (page == null) {
            return null;
        }
        Page<UserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }
}

