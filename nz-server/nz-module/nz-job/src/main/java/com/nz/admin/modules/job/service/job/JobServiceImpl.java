package com.nz.admin.modules.job.service.job;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.framework.quartz.core.QuartzCronUtils;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.mapper.job.JobMapper;
import com.nz.admin.modules.job.entity.vo.job.JobMutationResult;
import com.nz.admin.modules.job.entity.vo.job.JobExecutionReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 任务定义和调度意图原子提交，API 不把排队结果当成实际执行成功。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobMapper jobMapper;
    private final JobExecuteService jobExecuteService;
    private final JobDispatchService dispatch;
    private final JobExecutionService executions;
    private final JobTenantProvider jobTenantProvider;

    @Override
    public Page<JobDO> listPage(Integer pageNum, Integer pageSize, String jobName, String jobGroup, Integer status) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > 10000)
            throw new BusinessException("分页参数不正确");
        Page<JobDO> page = jobMapper.selectPageByCondition(new Page<>(pageNum, pageSize), jobName, jobGroup, status);
        dispatch.fillStates(page.getRecords());
        return page;
    }

    @Override
    public JobDO getById(Long id) {
        trustedTenantId();
        JobDO job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException("任务不存在或无权访问");
        dispatch.fillStates(List.of(job));
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMutationResult save(JobDO input) {
        JobDO job = writableFields(input);
        job.setId(null);
        job.setTenantId(trustedTenantId());
        job.setRevision(1L);
        normalizeAndValidate(job);
        if (jobMapper.insert(job) != 1) throw new BusinessException("任务创建失败");
        input.setId(job.getId());
        input.setRevision(job.getRevision());
        return dispatch.enqueue(job, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMutationResult updateById(JobDO input) {
        JobDO previous = lockJob(input.getId());
        JobDO job = writableFields(input);
        job.setTenantId(null);
        job.setRevision(previous.getRevision() + 1);
        normalizeAndValidate(job);
        if (jobMapper.updateById(job) != 1) throw new BusinessException("任务不存在或无权修改");
        job.setTenantId(previous.getTenantId());
        return dispatch.enqueue(job, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMutationResult removeById(Long id) {
        JobDO previous = lockJob(id);
        if (jobMapper.deleteById(id) != 1) throw new BusinessException("任务不存在或无权删除");
        previous.setRevision(previous.getRevision() + 1);
        return dispatch.enqueue(previous, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobExecutionReceipt runOnce(Long id) { return runOnce(id, null); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobExecutionReceipt runOnce(Long id, String requestKey) {
        JobDO job = lockJob(id);
        jobExecuteService.validateTarget(job.getInvokeTarget());
        dispatch.enqueue(job, false);
        return executions.enqueue(job, requestKey, jobExecuteService.supportsRecovery(job.getInvokeTarget()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMutationResult pauseJob(Long id) { return changeStatus(id, 1); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobMutationResult resumeJob(Long id) { return changeStatus(id, 0); }

    private JobMutationResult changeStatus(Long id, int status) {
        JobDO previous = lockJob(id);
        JobDO job = BeanUtil.copyProperties(previous, JobDO.class);
        job.setStatus(status);
        job.setRevision(previous.getRevision() + 1);
        if (status == 0) normalizeAndValidate(job);
        if (jobMapper.updateById(new JobDO().setId(id).setStatus(status).setRevision(job.getRevision())) != 1)
            throw new BusinessException("任务不存在或无权修改");
        return dispatch.enqueue(job, false);
    }

    @Override
    public void initializeScheduler(List<JobDO> jobs) {
        dispatch.initialize(jobs);
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

}
