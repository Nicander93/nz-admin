package com.nz.admin.modules.system.controller.log;

import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.excel.support.ExcelResponseUtils;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import com.nz.admin.modules.system.entity.vo.log.LoginLogExportVO;
import com.nz.admin.modules.system.service.log.LoginLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/system/login/log")
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @SaCheckPermission("system:loginlog:list")
    @GetMapping("/page")
    public R<PageResult<LoginLogDO>> page(LoginLogQuery query) {
        return R.ok(PageResult.of(loginLogService.listPage(query)));
    }

    @SaCheckPermission("system:loginlog:list")
    @Log(title = "登录日志", businessType = BusinessType.EXPORT, recordResponse = false)
    @GetMapping("/export")
    public void export(LoginLogQuery query, HttpServletResponse response) throws java.io.IOException {
        query.setPageNum(1);
        query.setPageSize(5000);
        java.util.List<LoginLogExportVO> rows = loginLogService.listPage(query).getRecords().stream()
                .map(this::toExportVO)
                .toList();
        ExcelResponseUtils.write(response, "login-log", "登录日志", LoginLogExportVO.class, rows);
    }

    @SaCheckPermission("system:loginlog:query")
    @GetMapping("/{id}")
    public R<LoginLogDO> getById(@PathVariable Long id) {
        return R.ok(loginLogService.getById(id));
    }

    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:loginlog:remove")
    @DeleteMapping
    public R<Void> delete(@RequestBody List<Long> ids) {
        loginLogService.removeByIds(ids);
        return R.ok();
    }

    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:loginlog:remove")
    @DeleteMapping("/clean")
    public R<Void> clean(@RequestParam(defaultValue = "30") int days) {
        int d = Math.max(1, days);
        loginLogService.removeBefore(LocalDateTime.now().minusDays(d));
        return R.ok();
    }

    private LoginLogExportVO toExportVO(LoginLogDO loginLog) {
        LoginLogExportVO vo = new LoginLogExportVO();
        vo.setId(loginLog.getId());
        vo.setUsername(loginLog.getUsername());
        vo.setIp(loginLog.getIp());
        vo.setStatus(loginLog.getStatus() != null && loginLog.getStatus() == 0 ? "成功" : "失败");
        vo.setMsg(loginLog.getMsg());
        vo.setLoginTime(loginLog.getLoginTime());
        return vo;
    }
}
