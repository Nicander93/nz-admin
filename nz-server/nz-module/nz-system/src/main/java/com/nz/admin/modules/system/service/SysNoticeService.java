package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysNotice;
import com.nz.admin.modules.system.query.SysNoticeQuery;

public interface SysNoticeService {

    Page<SysNotice> listPage(SysNoticeQuery query);

    SysNotice getById(Long id);

    boolean save(SysNotice notice);

    boolean updateById(SysNotice notice);

    boolean removeById(Long id);
}
