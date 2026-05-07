package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysNoticeDO;
import com.nz.admin.modules.system.entity.query.SysNoticeQuery;

public interface SysNoticeService {

    Page<SysNoticeDO> listPage(SysNoticeQuery query);

    SysNoticeDO getById(Long id);

    boolean save(SysNoticeDO notice);

    boolean updateById(SysNoticeDO notice);

    boolean removeById(Long id);
}
