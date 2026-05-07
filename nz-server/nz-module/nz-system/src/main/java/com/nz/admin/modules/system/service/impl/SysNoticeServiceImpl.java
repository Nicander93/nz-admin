package com.nz.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.modules.system.entity.po.SysNoticeDO;
import com.nz.admin.modules.system.mapper.SysNoticeMapper;
import com.nz.admin.modules.system.entity.query.SysNoticeQuery;
import com.nz.admin.modules.system.service.SysNoticeService;
import org.springframework.stereotype.Service;

/**
 * 通知公告服务实现。
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNoticeDO> implements SysNoticeService {

    /**
     * 按条件分页查通知公告。
     */
    @Override
    public Page<SysNoticeDO> listPage(SysNoticeQuery query) {
        return baseMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查询通知公告。
     */
    @Override
    public SysNoticeDO getById(Long id) {
        return super.getById(id);
    }

    /**
     * 新增一条通知公告。
     */
    @Override
    public boolean save(SysNoticeDO notice) {
        return super.save(notice);
    }

    /**
     * 按 id 更新通知公告。
     */
    @Override
    public boolean updateById(SysNoticeDO notice) {
        return super.updateById(notice);
    }

    /**
     * 按 id 删除通知公告。
     */
    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }
}
