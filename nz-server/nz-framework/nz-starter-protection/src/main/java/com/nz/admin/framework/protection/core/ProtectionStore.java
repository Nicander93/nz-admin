package com.nz.admin.framework.protection.core;

/** 原子接口保护存储；共享实现不得在故障时回退到本地。 */
public interface ProtectionStore {
    boolean isRepeatSubmit(String key, int intervalSeconds);

    boolean tryAcquire(String key, int permits, int windowSeconds);
}
