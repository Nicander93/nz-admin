package com.nz.admin.modules.system.controller.job;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.job.JobLogDO;
import com.nz.admin.modules.system.service.job.JobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务日志控制器。
 */
@RestController
@RequestMapping("/api/system/job/log")
public class JobLogController {

    @Autowired
    private JobLogService jobLogService;

    @SaCheckPermission("system:joblog:list")
    @GetMapping("/page")
    public R<PageResult<JobLogDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String jobName,
                                        @RequestParam(required = false) String jobGroup,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(jobLogService.listPage(pageNum, pageSize, jobName, jobGroup, status)));
    }

    @SaCheckPermission("system:joblog:query")
    @GetMapping("/{id}")
    public R<JobLogDO> getById(@PathVariable Long id) {
        return R.ok(jobLogService.getById(id));
    }

    @Log(title = "定时任务日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:joblog:remove")
    @DeleteMapping
    public R<Void> delete(@RequestBody List<Long> ids) {
        jobLogService.removeByIds(ids);
        return R.ok();
    }
}
