package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.po.SysJobDO;

public interface SysJobService {

    Page<SysJobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    SysJobDO getById(Long id);

    void save(SysJobDO job);

    void updateById(SysJobDO job);

    void removeById(Long id);

    /**
     * 立即执行一次任务。
     */
    void runOnce(Long id);

    /**
     * 暂停任务。
     */
    void pauseJob(Long id);

    /**
     * 恢复任务。
     */
    void resumeJob(Long id);
}
