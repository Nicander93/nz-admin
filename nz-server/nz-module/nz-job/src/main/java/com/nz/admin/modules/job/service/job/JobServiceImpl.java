package com.nz.admin.modules.job.service.job;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobInvocation;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.quartz.core.QuartzCronUtils;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.mapper.job.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/** 任务定义以数据库为准，调度失败回滚写入并恢复原调度配置。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final JobExecuteService jobExecuteService;
    private final QuartzJobService quartzJobService;
    private final JobTenantProvider jobTenantProvider;

    @Override
    public Page<JobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > 10000)
            throw new BusinessException("分页参数不正确");
        return jobMapper.selectPageByCondition(new Page<>(pageNum, pageSize), jobName, jobGroup, status);
    }

    @Override
    public JobDO getById(Long id) {
        JobDO job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException("任务不存在或无权访问");
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(JobDO input) {
        JobDO job = writableFields(input);
        job.setId(null);
        job.setTenantId(trustedTenantId());
        normalizeAndValidate(job);
        if (jobMapper.insert(job) != 1) throw new BusinessException("任务创建失败");
        input.setId(job.getId());
        synchronize(job, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(JobDO input) {
        JobDO previous = lockJob(input.getId());
        JobDO job = writableFields(input);
        job.setTenantId(null);
        normalizeAndValidate(job);
        if (jobMapper.updateById(job) != 1) throw new BusinessException("任务不存在或无权修改");
        job.setTenantId(previous.getTenantId());
        synchronize(job, previous);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        JobDO previous = lockJob(id);
        if (jobMapper.deleteById(id) != 1) throw new BusinessException("任务不存在或无权删除");
        synchronize(null, previous);
    }

    @Override
    public void runOnce(Long id) {
        trustedTenantId();
        JobDO job = getById(id);
        try { jobExecuteService.executeOrThrow(new JobInvocation(job.getId(), job.getJobName(), job.getJobGroup(), job.getInvokeTarget())); }
        catch (RuntimeException error) {
            var failure = new BusinessException("任务执行失败，请查看任务日志");
            failure.initCause(error);
            throw failure;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pauseJob(Long id) { changeStatus(id, 1); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeJob(Long id) { changeStatus(id, 0); }

    private void changeStatus(Long id, int status) {
        JobDO previous = lockJob(id);
        JobDO job = BeanUtil.copyProperties(previous, JobDO.class);
        job.setStatus(status);
        if (status == 0) normalizeAndValidate(job);
        if (jobMapper.updateById(new JobDO().setId(id).setStatus(status)) != 1)
            throw new BusinessException("任务不存在或无权修改");
        synchronize(job, previous);
    }

    @Override
    public void initializeScheduler(List<JobDO> jobs) {
        quartzJobService.scheduleAllJobs(jobs);
    }

    private JobDO lockJob(Long id) {
        trustedTenantId();
        if (id == null) throw new BusinessException("请选择任务");
        JobDO job = jobMapper.selectOne(new LambdaQueryWrapper<JobDO>().eq(JobDO::getId, id).last("FOR UPDATE"));
        if (job == null) throw new BusinessException("任务不存在或无权访问");
        return job;
    }

    private Long trustedTenantId() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) throw new BusinessException("任务操作需要可信租户上下文");
        if (!jobTenantProvider.isEnabledTenant(tenantId)) throw new BusinessException(403, "租户已停用或过期，不能操作任务");
        return tenantId;
    }

    private JobDO writableFields(JobDO input) {
        return new JobDO().setId(input.getId()).setJobName(input.getJobName()).setJobGroup(input.getJobGroup())
                .setInvokeTarget(input.getInvokeTarget()).setCronExpression(input.getCronExpression())
                .setStatus(input.getStatus()).setConcurrent(input.getConcurrent())
                .setMisfirePolicy(input.getMisfirePolicy()).setRemark(input.getRemark());
    }

    private void normalizeAndValidate(JobDO job) {
        if (StrUtil.isBlank(job.getJobName()) || job.getJobName().length() > 64) throw new BusinessException("任务名称不正确");
        if (job.getCronExpression() == null || job.getCronExpression().length() > 64 || !QuartzCronUtils.isValid(job.getCronExpression())) throw new BusinessException("Cron 表达式不正确");
        job.setJobGroup(StrUtil.blankToDefault(job.getJobGroup(), "DEFAULT"));
        if (job.getJobGroup().length() > 64) throw new BusinessException("任务分组过长");
        if (job.getStatus() == null) job.setStatus(1);
        if (job.getConcurrent() == null) job.setConcurrent(1);
        if (job.getMisfirePolicy() == null) job.setMisfirePolicy(3);
        if (job.getStatus() < 0 || job.getStatus() > 1 || job.getConcurrent() < 0 || job.getConcurrent() > 1
                || job.getMisfirePolicy() < 1 || job.getMisfirePolicy() > 3) throw new BusinessException("任务状态或策略不正确");
        try { jobExecuteService.validateTarget(job.getInvokeTarget()); }
        catch (RuntimeException error) { throw new BusinessException("调用目标必须是已注册且带 @JobTask 的 public 无参方法"); }
    }

    private void synchronize(JobDO desired, JobDO previous) {
        Long id = desired == null ? previous.getId() : desired.getId();
        // Quartz 与业务库不是同一个事务；回滚时补偿，重启时再按库中状态恢复。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return;
                try {
                    if (previous == null) quartzJobService.removeById(id);
                    else quartzJobService.scheduleJobIfActive(previous);
                } catch (RuntimeException error) {
                    log.error("回滚后恢复 Quartz 失败，需恢复调度器并重启同步 id={}", id, error);
                }
            }
        });
        if (desired == null) quartzJobService.removeById(id);
        else quartzJobService.scheduleJobIfActive(desired);
    }
}
