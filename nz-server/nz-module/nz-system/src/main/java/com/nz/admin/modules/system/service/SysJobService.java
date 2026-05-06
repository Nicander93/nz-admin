package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysJob;

public interface SysJobService {

    Page<SysJob> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status);

    SysJob getById(Long id);

    void save(SysJob job);

    void updateById(SysJob job);

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
