package com.nz.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.po.SysJobDO;
import com.nz.admin.modules.system.service.SysJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务控制器。
 */
@RestController
@RequestMapping("/api/system/job")
public class SysJobController {

    @Autowired
    private SysJobService jobService;

    @SaCheckPermission("system:job:list")
    @GetMapping("/page")
    public R<Page<SysJobDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) String jobName,
                                @RequestParam(required = false) String jobGroup,
                                @RequestParam(required = false) Integer status) {
        return R.ok(jobService.listPage(pageNum, pageSize, jobName, jobGroup, status));
    }

    @SaCheckPermission("system:job:query")
    @GetMapping("/{id}")
    public R<SysJobDO> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @Log(title = "定时任务", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:job:add")
    @PostMapping
    public R<Void> add(@RequestBody SysJobDO job) {
        jobService.save(job);
        return R.ok();
    }

    @Log(title = "定时任务", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:job:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysJobDO job) {
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
