package com.nz.admin.common.job;

/** 当前任务上下文仅在执行范围内可见，线程复用时还原上一层。 */
public final class JobRunContextHolder {
    private static final ThreadLocal<JobRunContext> CURRENT = new ThreadLocal<>();
    private JobRunContextHolder() {}
    public static JobRunContext currentOrNull() { return CURRENT.get(); }
    public static JobRunContext requireCurrent() {
        JobRunContext value = CURRENT.get();
        if (value == null) throw new IllegalStateException("当前线程没有任务执行上下文");
        return value;
    }
    public static void runWith(JobRunContext context, Runnable action) {
        JobRunContext previous = CURRENT.get();
        if (context == null) CURRENT.remove(); else CURRENT.set(context);
        try { action.run(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
