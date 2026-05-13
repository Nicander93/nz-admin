package com.nz.admin.framework.quartz.core;

import org.quartz.CronExpression;

/**
 * Cron 工具。
 */
public final class QuartzCronUtils {

    private QuartzCronUtils() {
    }

    public static boolean isValid(String cronExpression) {
        return cronExpression != null && !cronExpression.isBlank() && CronExpression.isValidExpression(cronExpression.trim());
    }
}
