package com.eldercare.backend.belongings.scheduling;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationTimers;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Việc theo phút của M12 ({@code ViecTheoPhutM12}; 013 T006, research K-15): xác nhận người nhận khác Hiệu lực quá
 * CFG-M10-08 chuyển Hết hạn, yêu cầu Chờ xác nhận đủ điều kiện duyệt thay báo Quản lý viện một lần (FR-017a, FR-017b).
 * Chỉ chuyển trạng thái theo giờ và báo, không lập bản ghi nghiệp vụ (Q-306, M12ArchTest). Mỗi lần chạy xử lý mọi việc
 * đã đến hạn tới lúc chạy, nên lần chạy lúc khởi động là chạy bù sau thời gian tắt; mỗi việc chống trùng bằng trạng thái
 * yêu cầu và dấu nhắc (NFR-04).
 */
@Component
public class M12MinuteJob implements ScheduledJob {

    static final String NAME = "M12_MINUTE";

    private static final Logger log = LoggerFactory.getLogger(M12MinuteJob.class);

    private final RecipientConfirmationTimers timers;
    private final SchedulerRuns runs;
    private final boolean onStartup;

    public M12MinuteJob(
            RecipientConfirmationTimers timers,
            SchedulerRuns runs,
            @Value("${eldercare.jobs.m12-minute.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.timers = timers;
        this.runs = runs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m12-minute.cron:0 * * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = NAME)
    public void scheduled() {
        run();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = NAME)
    public void atStartup() {
        if (onStartup) {
            run();
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void runNow() {
        run();
    }

    /** Hai việc chạy độc lập: việc này lỗi thì việc kia vẫn chạy, lần chạy ghi FAILED. */
    private void run() {
        String runId = runs.start(NAME);
        int processed = 0;
        boolean failed = false;
        try {
            processed += timers.expireDue();
        } catch (RuntimeException e) {
            failed = true;
            log.warn("Việc hết hạn xác nhận người nhận của {} lỗi", NAME, e);
        }
        try {
            processed += timers.notifyEligible();
        } catch (RuntimeException e) {
            failed = true;
            log.warn("Việc báo đủ điều kiện duyệt thay của {} lỗi", NAME, e);
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }
}
