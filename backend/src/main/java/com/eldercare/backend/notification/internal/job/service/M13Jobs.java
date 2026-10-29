package com.eldercare.backend.notification.internal.job.service;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.job.persistence.JobRunStore;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Chạy việc của hai job M13 (research B-10). Mỗi việc chạy riêng: một việc lỗi không chặn việc khác, lần chạy ghi
 * {@code FAILED}. Job hằng ngày chạy từ 07:30 (giờ Việt Nam; thông số vận hành) và mỗi ngày nghiệp vụ một lần, nên lần
 * khởi động sau 07:30 chạy bù ngày chưa chạy (NFR-04).
 */
@Service
public class M13Jobs {

    public static final String FAST = "M13_FAST";
    public static final String DAILY = "M13_DAILY";
    static final LocalTime DAILY_AT = LocalTime.of(7, 30);

    private static final Logger log = LoggerFactory.getLogger(M13Jobs.class);

    private final List<M13FastTask> fastTasks;
    private final List<M13DailyTask> dailyTasks;
    private final SchedulerRuns runs;
    private final JobRunStore store;
    private final BusinessTime time;

    M13Jobs(
            List<M13FastTask> fastTasks,
            List<M13DailyTask> dailyTasks,
            SchedulerRuns runs,
            JobRunStore store,
            BusinessTime time) {
        this.fastTasks = fastTasks;
        this.dailyTasks = dailyTasks;
        this.runs = runs;
        this.store = store;
        this.time = time;
    }

    /** Một lượt job 20 giây; trả id lần chạy. */
    public long runFast() {
        long runId = runs.start(FAST);
        int processed = 0;
        boolean failed = false;
        for (M13FastTask task : fastTasks) {
            try {
                processed += task.run();
            } catch (RuntimeException e) {
                failed = true;
                log.warn("Việc {} của {} lỗi", task.name(), FAST, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }

    /** Chạy job hằng ngày cho hôm nay nếu đã tới giờ và hôm nay chưa chạy xong. */
    public void runDailyIfDue() {
        Instant now = time.now();
        LocalDate today = time.today();
        if (now.isBefore(today.atTime(DAILY_AT).atZone(BusinessTime.ZONE).toInstant())
                || store.completed(DAILY, today)) {
            return;
        }
        long runId = runs.start(DAILY);
        int processed = 0;
        boolean failed = false;
        for (M13DailyTask task : dailyTasks) {
            try {
                processed += task.run(today);
            } catch (RuntimeException e) {
                failed = true;
                log.warn("Việc {} của {} lỗi", task.name(), DAILY, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        store.markCompleted(DAILY, today, runId, time.now());
    }
}
