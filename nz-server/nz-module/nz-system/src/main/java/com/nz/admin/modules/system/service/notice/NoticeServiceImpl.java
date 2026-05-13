package com.nz.admin.modules.system.service.notice;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nz.admin.modules.system.entity.dataobject.notice.NoticeDO;
import com.nz.admin.modules.system.mapper.notice.NoticeMapper;
import com.nz.admin.modules.system.entity.query.notice.NoticeQuery;
import com.nz.admin.modules.system.service.notice.NoticeService;
import org.springframework.stereotype.Service;

/**
 * 通知公告服务实现。
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, NoticeDO> implements NoticeService {

    /**
     * 按条件分页查通知公告。
     */
    @Override
    public Page<NoticeDO> listPage(NoticeQuery query) {
        return baseMapper.selectPageByCondition(query.toPage(), query);
    }

    /**
     * 按 id 查询通知公告。
     */
    @Override
    public NoticeDO getById(Long id) {
        return super.getById(id);
    }

    /**
     * 新增一条通知公告。
     */
    @Override
    public boolean save(NoticeDO notice) {
        return super.save(notice);
    }

    /**
     * 按 id 更新通知公告。
     */
    @Override
    public boolean updateById(NoticeDO notice) {
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
