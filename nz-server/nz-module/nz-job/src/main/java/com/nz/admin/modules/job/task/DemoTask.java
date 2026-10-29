package com.nz.admin.modules.job.task;

import com.nz.admin.common.job.JobTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 与基线 demoTask.run 占位记录兼容的无副作用演示任务。 */
@Slf4j
@Component("demoTask")
public class DemoTask {
    @JobTask(recoverable = true)
    public void run() {
        log.info("演示任务执行完成");
    }
}
