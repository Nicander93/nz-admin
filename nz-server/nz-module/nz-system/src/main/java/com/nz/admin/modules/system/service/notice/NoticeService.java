package com.nz.admin.modules.system.service.notice;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.notice.NoticeDO;
import com.nz.admin.modules.system.entity.query.notice.NoticeQuery;

public interface NoticeService {

    Page<NoticeDO> listPage(NoticeQuery query);

    NoticeDO getById(Long id);

    boolean save(NoticeDO notice);

    boolean updateById(NoticeDO notice);

    boolean removeById(Long id);
}
