package com.nz.admin.modules.system.job;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.system.entity.po.SysJobLogDO;
import com.nz.admin.modules.system.service.SysJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务执行器。
 */
@Component
public class JobExecuteService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SysJobLogService jobLogService;

    /**
     * 执行指定目标方法。
     * invokeTarget 格式：beanName.methodName
     */
    public void execute(String invokeTarget) {
        SysJobLogDO jobLog = new SysJobLogDO();
        jobLog.setInvokeTarget(invokeTarget);
        jobLog.setCreateTime(LocalDateTime.now());

        if (StrUtil.isBlank(invokeTarget)) {
            jobLog.setStatus(1);
            jobLog.setJobMessage("调用目标不能为空");
            jobLogService.save(jobLog);
            return;
        }

        String[] parts = invokeTarget.split("\\.");
        if (parts.length < 2) {
            jobLog.setStatus(1);
            jobLog.setJobMessage("调用目标格式错误，应为 beanName.methodName");
            jobLogService.save(jobLog);
            return;
        }

        String beanName = parts[0];
        String methodName = parts[1];

        try {
            Object bean = applicationContext.getBean(beanName);
            java.lang.reflect.Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);

            jobLog.setJobMessage("执行成功");
            jobLog.setStatus(0);
        } catch (Exception e) {
            jobLog.setStatus(1);
            jobLog.setJobMessage("执行失败：" + e.getMessage());
            jobLog.setExceptionInfo(StrUtil.subPre(e.getMessage(), 2000));
        }

        jobLogService.save(jobLog);
    }
}
