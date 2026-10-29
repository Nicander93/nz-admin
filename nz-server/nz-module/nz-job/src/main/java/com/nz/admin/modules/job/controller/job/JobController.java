package com.nz.admin.modules.job.controller.job;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.quartz.core.QuartzCronUtils;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.entity.dto.job.JobWriteRequest;
import com.nz.admin.modules.job.entity.vo.job.JobMutationResult;
import com.nz.admin.modules.job.entity.vo.job.JobExecutionReceipt;
import com.nz.admin.modules.job.entity.dataobject.job.JobExecutionDO;
import com.nz.admin.modules.job.entity.dataobject.job.JobScheduleOutboxDO;
import com.nz.admin.modules.job.service.job.JobDispatchService;
import com.nz.admin.modules.job.service.job.JobExecutionService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import com.nz.admin.modules.job.service.job.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务控制器。
 */
@Tag(name = "定时任务")
@RestController
@RequestMapping("/api/system/job")
public class JobController {

    @Autowired
    private JobService jobService;
    @Autowired private JobDispatchService dispatch;
    @Autowired private JobExecutionService executions;

    @Operation(summary = "校验 Cron 表达式")
    @SaCheckPermission("system:job:query")
    @GetMapping("/cron-valid")
    public R<Boolean> validateCron(@RequestParam(required = false) String cron) {
        if (cron == null || cron.isBlank()) {
            return R.ok(false);
        }
        return R.ok(QuartzCronUtils.isValid(cron));
    }

    @Operation(summary = "分页查询定时任务")
    @SaCheckPermission("system:job:list")
    @GetMapping("/page")
    public R<PageResult<JobDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) String jobName,
                                     @RequestParam(required = false) String jobGroup,
                                     @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(jobService.listPage(pageNum, pageSize, jobName, jobGroup, status)));
    }

    @SaCheckPermission("system:job:query")
    @GetMapping("/{id}")
    public R<JobDO> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @Log(title = "定时任务", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:job:add")
    @PostMapping
    public R<JobMutationResult> add(@Valid @RequestBody JobWriteRequest request) {
        return R.ok(jobService.save(request.toEntity()));
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping
    public R<JobMutationResult> update(@Validated(JobWriteRequest.Update.class) @RequestBody JobWriteRequest request) {
        return R.ok(jobService.updateById(request.toEntity()));
    }

    @Log(title = "定时任务", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:job:remove")
    @DeleteMapping("/{id}")
    public R<JobMutationResult> delete(@PathVariable Long id) {
        return R.ok(jobService.removeById(id));
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/run/{id}")
    public R<JobExecutionReceipt> runOnce(@PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String requestKey) {
        return R.ok(jobService.runOnce(id, requestKey));
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/pause/{id}")
    public R<JobMutationResult> pause(@PathVariable Long id) {
        return R.ok(jobService.pauseJob(id));
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/resume/{id}")
    public R<JobMutationResult> resume(@PathVariable Long id) {
        return R.ok(jobService.resumeJob(id));
    }

    @SaCheckPermission("system:job:list")
    @GetMapping("/scheduler")
    public R<java.util.Map<String, Object>> schedulerState() { return R.ok(dispatch.schedulerState()); }

    @SaCheckPermission("system:job:list")
    @GetMapping("/schedule-outbox/page")
    public R<PageResult<JobScheduleOutboxDO>> schedules(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String state) {
        return R.ok(PageResult.of(dispatch.list(pageNum, pageSize, jobId, state)));
    }

    @SaCheckPermission("system:job:edit")
    @PutMapping("/schedule-outbox/{id}/retry")
    @Log(title = "重试任务调度同步", businessType = BusinessType.UPDATE)
    public R<JobMutationResult> retrySchedule(@PathVariable Long id) { return R.ok(dispatch.retry(id)); }

    @SaCheckPermission("system:joblog:list")
    @GetMapping("/execution/page")
    public R<PageResult<JobExecutionDO>> executions(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String state, @RequestParam(required = false) String executionKey) {
        return R.ok(PageResult.of(executions.list(pageNum, pageSize, jobId, state, executionKey)));
    }

    @SaCheckPermission("system:joblog:query")
    @GetMapping("/execution/{id}")
    public R<JobExecutionDO> execution(@PathVariable Long id) { return R.ok(executions.getCurrent(id)); }

    @SaCheckPermission("system:job:edit")
    @PutMapping("/execution/{id}/retry")
    @Log(title = "重试可恢复任务执行", businessType = BusinessType.UPDATE)
    public R<JobExecutionReceipt> retryExecution(@PathVariable Long id) { return R.ok(executions.retry(id)); }
}
