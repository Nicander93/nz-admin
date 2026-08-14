package @@PACKAGE@@.convert;

import cn.hutool.core.bean.BeanUtil;
import @@PACKAGE@@.entity.dataobject.@@CLASS@@DO;
import @@PACKAGE@@.entity.vo.@@CLASS@@VO;

import java.util.List;

/**
 * @@FEATURE_DOC@@对象转换。
 *
 * @author @@AUTHOR@@
 */
public final class @@CLASS@@Convert {

    private @@CLASS@@Convert() {
    }

    public static @@CLASS@@VO toVO(@@CLASS@@DO source) {
        return source == null ? null : BeanUtil.toBean(source, @@CLASS@@VO.class);
    }

    public static List<@@CLASS@@VO> toVOList(List<@@CLASS@@DO> source) {
        return source == null ? List.of() : source.stream().map(@@CLASS@@Convert::toVO).toList();
    }
}
