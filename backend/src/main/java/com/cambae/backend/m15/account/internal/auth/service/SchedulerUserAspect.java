package com.cambae.backend.m15.account.internal.auth.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Đặt Bộ lập lịch làm người dùng hiện tại trong lúc job chạy (002 T012; contracts/module-interfaces.md mục 1.1): mọi
 * phương thức {@code @Scheduled} và {@code ScheduledJob.runNow} (chạy theo yêu cầu kiểm thử, NFR-13). Job chạy trong một
 * yêu cầu có phiên vẫn là Bộ lập lịch; xong thì trả lại như trước.
 */
@Aspect
@Component
class SchedulerUserAspect {

    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> false);

    static boolean isRunning() {
        return RUNNING.get();
    }

    /**
     * Chạy {@code work} với người dùng hiện tại là Bộ lập lịch: tác động kèm lệnh của module khác mà nhật ký ghi người
     * thực hiện là Bộ lập lịch kèm căn cứ (002 T032 {@code lockOnResignation}; 000 FR-045).
     */
    static void runAsScheduler(Runnable work) {
        boolean outer = RUNNING.get();
        RUNNING.set(true);
        try {
            work.run();
        } finally {
            RUNNING.set(outer);
        }
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)"
            + " || execution(* com.cambae.backend.common.scheduling.ScheduledJob+.runNow())")
    public Object runAsScheduler(ProceedingJoinPoint job) throws Throwable {
        boolean outer = RUNNING.get();
        RUNNING.set(true);
        try {
            return job.proceed();
        } finally {
            RUNNING.set(outer);
        }
    }
}
