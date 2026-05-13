package com.nz.admin.modules.system.service.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;
import com.nz.admin.modules.system.mapper.job.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时任务服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final JobExecuteService jobExecuteService;
    private final QuartzJobService quartzJobService;

    @Override
    public Page<JobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        Page<JobDO> page = new Page<>(pageNum, pageSize);
        return jobMapper.selectPageByCondition(page, jobName, jobGroup, status);
    }

    @Override
    public JobDO getById(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public void save(JobDO job) {
        jobMapper.insert(job);
        if (job.getStatus() != null && job.getStatus() == 0 && StrUtil.isNotBlank(job.getCronExpression())) {
            try {
                quartzJobService.scheduleJob(job);
            } catch (SchedulerException e) {
                log.error("新增任务后注册 Quartz 失败 id={}", job.getId(), e);
            }
        }
    }

    @Override
    public void updateById(JobDO job) {
        jobMapper.updateById(job);
        if (job.getId() != null) {
            JobDO full = jobMapper.selectById(job.getId());
            if (full != null) {
                quartzJobService.scheduleJobIfActive(full);
            }
        }
    }

    @Override
    public void removeById(Long id) {
        jobMapper.deleteById(id);
        quartzJobService.removeById(id);
    }

    @Override
    public void runOnce(Long id) {
        JobDO job = jobMapper.selectById(id);
        if (job != null && StrUtil.isNotBlank(job.getInvokeTarget())) {
            jobExecuteService.execute(job.getInvokeTarget());
        }
    }

    @Override
    public void pauseJob(Long id) {
        JobDO patch = new JobDO();
        patch.setId(id);
        patch.setStatus(1);
        jobMapper.updateById(patch);
        JobDO full = jobMapper.selectById(id);
        if (full != null) {
            quartzJobService.scheduleJobIfActive(full);
        }
    }

    @Override
    public void resumeJob(Long id) {
        JobDO patch = new JobDO();
        patch.setId(id);
        patch.setStatus(0);
        jobMapper.updateById(patch);
        JobDO full = jobMapper.selectById(id);
        if (full != null) {
            quartzJobService.scheduleJobIfActive(full);
        }
    }

    @Override
    public void initializeScheduler(List<JobDO> jobs) {
        quartzJobService.scheduleAllJobs(jobs);
    }
}
