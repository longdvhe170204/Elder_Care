package com.eldercare.backend.health.internal.scheduling.service;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.scheduling.persistence.M06JobRunStore;
import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask.Cadence;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Khung hai job M06: ghi lần chạy vào jobs.log và chỉ đánh dấu ngày khi mọi tác vụ hằng ngày thành công. */
@Service
public class M06Jobs {
    public static final String MINUTE = "M06_MINUTE";
    public static final String DAILY = "M06_DAILY";
    static final LocalTime DAILY_AT = LocalTime.of(0, 15);

    private static final Logger log = LoggerFactory.getLogger(M06Jobs.class);

    private final List<M06TimedTask> tasks;
    private final SchedulerRuns runs;
    private final M06JobRunStore store;
    private final BusinessTime time;

    public M06Jobs(List<M06TimedTask> tasks, SchedulerRuns runs, M06JobRunStore store, BusinessTime time) {
        this.tasks = tasks;
        this.runs = runs;
        this.store = store;
        this.time = time;
    }

    public void runMinute() {
        Instant actual = time.now();
        run(Cadence.MINUTE, actual.truncatedTo(ChronoUnit.MINUTES), actual, null);
    }

    public void runDailyIfDue() {
        Instant actual = time.now();
        LocalDate today = time.today();
        Instant todayDue = today.atTime(DAILY_AT).atZone(BusinessTime.ZONE).toInstant();
        LocalDate dueDay = actual.isBefore(todayDue) ? today.minusDays(1) : today;
        if (store.completed(DAILY, dueDay)) return;
        run(Cadence.DAILY, dueDay.atTime(DAILY_AT).atZone(BusinessTime.ZONE).toInstant(), actual, dueDay);
    }

    private void run(Cadence cadence, Instant scheduledAt, Instant actualAt, LocalDate dueDay) {
        String job = cadence == Cadence.MINUTE ? MINUTE : DAILY;
        String runId = runs.start(job);
        int processed = 0;
        boolean failed = false;
        for (M06TimedTask task : tasks) {
            if (task.cadence() != cadence) continue;
            try {
                processed += task.run(scheduledAt, actualAt);
            } catch (RuntimeException exception) {
                failed = true;
                log.warn("Việc {} của {} lỗi", task.name(), job, exception);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        if (!failed && dueDay != null) store.markCompleted(job, dueDay, runId);
    }
}
