package com.nz.admin.modules.demo.convert;

import cn.hutool.core.bean.BeanUtil;
import com.nz.admin.modules.demo.entity.dataobject.DemoItemDO;
import com.nz.admin.modules.demo.entity.dto.DemoItemCreateRequest;
import com.nz.admin.modules.demo.entity.dto.DemoItemUpdateRequest;
import com.nz.admin.modules.demo.entity.vo.DemoItemVO;

import java.util.List;

/**
 * 示例条目对象转换。
 */
public final class DemoItemConvert {

    private DemoItemConvert() {
    }

    public static DemoItemDO toDO(DemoItemCreateRequest request) {
        return BeanUtil.copyProperties(request, DemoItemDO.class);
    }

    public static DemoItemDO toDO(DemoItemUpdateRequest request) {
        return BeanUtil.copyProperties(request, DemoItemDO.class);
    }

    public static DemoItemVO toVO(DemoItemDO item) {
        return BeanUtil.copyProperties(item, DemoItemVO.class);
    }

    public static List<DemoItemVO> toVOList(List<DemoItemDO> items) {
        return items.stream().map(DemoItemConvert::toVO).toList();
    }
}
