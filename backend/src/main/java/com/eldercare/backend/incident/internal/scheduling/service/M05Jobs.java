package com.eldercare.backend.incident.internal.scheduling.service;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Khung việc theo phút M05; mỗi tác vụ lỗi không chặn tác vụ khác và lần chạy được ghi FAILED. */
@Service
public class M05Jobs {
    public static final String MINUTE = "M05_MINUTE";

    private static final Logger log = LoggerFactory.getLogger(M05Jobs.class);

    private final List<M05MinuteTask> tasks;
    private final SchedulerRuns runs;
    private final BusinessTime time;

    public M05Jobs(List<M05MinuteTask> tasks, SchedulerRuns runs, BusinessTime time) {
        this.tasks = tasks;
        this.runs = runs;
        this.time = time;
    }

    public void runMinute() {
        Instant actual = time.now();
        Instant scheduled = actual.truncatedTo(ChronoUnit.MINUTES);
        String runId = runs.start(MINUTE);
        int processed = 0;
        boolean failed = false;
        for (M05MinuteTask task : tasks) {
            try {
                processed += task.run(scheduled, actual);
            } catch (RuntimeException exception) {
                failed = true;
                log.warn("Việc {} của {} lỗi", task.name(), MINUTE, exception);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }
}
