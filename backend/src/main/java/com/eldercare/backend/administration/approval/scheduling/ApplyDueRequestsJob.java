package com.eldercare.backend.administration.approval.scheduling;

import com.eldercare.backend.administration.approval.internal.service.DueRequestApplication;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job áp dụng yêu cầu Đã duyệt đến hạn (000 T093, research R-10): 15 phút một lần (gồm 00:05) và lúc ứng dụng khởi
 * động, để bù các ngày Bộ lập lịch gián đoạn (FR-037a). Lịch chạy là thông số kỹ thuật, không phải CFG; profile test tắt
 * lịch và lần chạy lúc khởi động, test gọi {@link #run()}.
 */
@Component
public class ApplyDueRequestsJob implements ScheduledJob {

    private final DueRequestApplication application;
    private final boolean onStartup;

    public ApplyDueRequestsJob(
            DueRequestApplication application,
            @Value("${eldercare.jobs.apply-due-requests.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean onStartup) {
        this.application = application;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.apply-due-requests.cron:0 5/15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "APPLY_DUE_REQUESTS")
    public void scheduled() {
        application.run();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "APPLY_DUE_REQUESTS")
    public void atStartup() {
        if (onStartup) {
            application.run();
        }
    }

    /** Chạy một đợt, trả mã lần chạy (UUID). */
    public String run() {
        return application.run();
    }

    @Override
    public String name() {
        return "APPLY_DUE_REQUESTS";
    }

    @Override
    public void runNow() {
        run();
    }
}
