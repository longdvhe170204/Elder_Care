package com.eldercare.backend.belongings.scheduling;

import com.eldercare.backend.belongings.internal.ending.service.FinalStateReminders;
import com.eldercare.backend.belongings.internal.inventorycheck.service.InventoryCheckReminders;
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
 * Việc hằng ngày 00:20 của M12 ({@code ViecHangNgayM12}; 013 T006, research K-15): nhắc đồ chưa trả sau trạng thái cuối
 * theo CFG-M12-02, báo đồ đủ CFG-M12-03 để lập đề nghị xử lý (FR-027, FR-027a), nhắc kiểm kê đầu kỳ và sau hạn
 * (FR-028a, FR-028c). Chỉ nhắc, không lập phiếu kiểm kê, không ghi sự cố (Q-306, Q-307, M12ArchTest). Mỗi lần chạy xét
 * trạng thái hiện tại và chống trùng bằng dấu nhắc ({@code reminder_marker}), nên lần chạy lúc khởi động là chạy bù đúng
 * một lần sau thời gian tắt (NFR-04, quickstart Q46).
 */
@Component
public class M12DailyJob implements ScheduledJob {

    static final String NAME = "M12_DAILY";

    private static final Logger log = LoggerFactory.getLogger(M12DailyJob.class);

    private final FinalStateReminders finalStateReminders;
    private final InventoryCheckReminders inventoryCheckReminders;
    private final SchedulerRuns runs;
    private final boolean onStartup;

    public M12DailyJob(
            FinalStateReminders finalStateReminders,
            InventoryCheckReminders inventoryCheckReminders,
            SchedulerRuns runs,
            @Value("${eldercare.jobs.m12-daily.on-startup:${eldercare.scheduling.enabled:true}}") boolean onStartup) {
        this.finalStateReminders = finalStateReminders;
        this.inventoryCheckReminders = inventoryCheckReminders;
        this.runs = runs;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m12-daily.cron:0 20 0 * * *}", zone = "Asia/Ho_Chi_Minh")
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
            processed += finalStateReminders.remindUnreturned();
        } catch (RuntimeException e) {
            failed = true;
            log.warn("Việc nhắc đồ chưa trả của {} lỗi", NAME, e);
        }
        try {
            inventoryCheckReminders.remind();
        } catch (RuntimeException e) {
            failed = true;
            log.warn("Việc nhắc kiểm kê của {} lỗi", NAME, e);
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }
}
