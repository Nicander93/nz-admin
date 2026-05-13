package com.nz.admin.modules.system.controller.job;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.framework.quartz.core.QuartzCronUtils;
import com.nz.admin.modules.system.entity.dataobject.job.JobDO;
import com.nz.admin.modules.system.service.job.JobService;
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
    public R<Void> add(@RequestBody JobDO job) {
        jobService.save(job);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping
    public R<Void> update(@RequestBody JobDO job) {
        jobService.updateById(job);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:job:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        jobService.removeById(id);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/run/{id}")
    public R<Void> runOnce(@PathVariable Long id) {
        jobService.runOnce(id);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/pause/{id}")
    public R<Void> pause(@PathVariable Long id) {
        jobService.pauseJob(id);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping("/resume/{id}")
    public R<Void> resume(@PathVariable Long id) {
        jobService.resumeJob(id);
        return R.ok();
    }
}
