package com.nz.admin.modules.system.controller.log;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import com.nz.admin.modules.system.service.log.OperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/system/oper/log")
public class OperLogController {

    @Autowired
    private OperLogService operLogService;

    @SaCheckPermission("system:operlog:list")
    @GetMapping("/page")
    public R<PageResult<OperLogDO>> page(OperLogQuery query) {
        return R.ok(PageResult.of(operLogService.listPage(query)));
    }

    @SaCheckPermission("system:operlog:query")
    @GetMapping("/{id}")
    public R<OperLogDO> getById(@PathVariable Long id) {
        return R.ok(operLogService.getById(id));
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:operlog:remove")
    @DeleteMapping
    public R<Void> delete(@RequestBody List<Long> ids) {
        operLogService.removeByIds(ids);
        return R.ok();
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:operlog:remove")
    @DeleteMapping("/clean")
    public R<Void> clean(@RequestParam(defaultValue = "30") int days) {
        int d = Math.max(1, days);
        operLogService.removeBefore(LocalDateTime.now().minusDays(d));
        return R.ok();
    }
}
