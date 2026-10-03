package com.nz.admin.framework.protection.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class InMemoryProtectionStoreTest {
    @Test
    void onlyOneConcurrentSubmissionMayEnter() throws Exception {
        var store = new InMemoryProtectionStore();
        var start = new CountDownLatch(1);
        var passed = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(16);
        try {
            var tasks = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 64; i++)
                tasks.add(
                        pool.submit(
                                () -> {
                                    try {
                                        start.await();
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                    if (!store.isRepeatSubmit("same", 60)) passed.incrementAndGet();
                                }));
            start.countDown();
            for (var task : tasks) task.get(5, TimeUnit.SECONDS);
            assertThat(passed.get()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rateLimitPermitsAreNotLostUnderContention() throws Exception {
        var store = new InMemoryProtectionStore();
        var passed = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(16);
        try {
            var tasks = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 100; i++)
                tasks.add(
                        pool.submit(
                                () -> {
                                    if (store.tryAcquire("same", 10, 60)) passed.incrementAndGet();
                                }));
            for (var task : tasks) task.get(5, TimeUnit.SECONDS);
            assertThat(passed.get()).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }
}
