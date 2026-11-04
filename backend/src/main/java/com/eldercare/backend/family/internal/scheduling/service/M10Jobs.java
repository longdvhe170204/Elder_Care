package com.eldercare.backend.family.internal.scheduling.service;

import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.scheduling.persistence.M10JobRunStore;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask.Cadence;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Khung việc theo phút, theo giờ, nửa đêm 00:00 của M10 ({@code ViecTheoPhutM10}, {@code ViecHangGioM10},
 * {@code ViecNuaDemM10}; research F-17, NFR-04). Mỗi (tác vụ, mốc) giành dấu {@code M10.JOB_RUN} ở {@code reminder_marker}
 * (R-15) trong transaction riêng cùng tác vụ, nên chạy đúng một lần kể cả khi hai tiến trình cùng chạy; tác vụ lỗi thì
 * dấu không ghi, mốc chạy lại ở lần sau, tác vụ khác vẫn chạy. Theo phút, theo giờ chỉ chạy mốc hiện tại (tác vụ xử lý
 * mọi việc đã đến hạn); nửa đêm chạy bù từng đêm từ đêm kế tiếp đêm đã xong gần nhất, lần đầu chỉ đêm mới nhất.
 */
@Service
public class M10Jobs {

    private static final Logger log = LoggerFactory.getLogger(M10Jobs.class);

    private final ObjectProvider<M10TimedTask> tasks;
    private final M10JobRunStore store;
    private final SchedulerRuns runs;
    private final BusinessTime time;
    private final TransactionTemplate transaction;

    M10Jobs(
            ObjectProvider<M10TimedTask> tasks,
            M10JobRunStore store,
            SchedulerRuns runs,
            BusinessTime time,
            PlatformTransactionManager transactions) {
        this.tasks = tasks;
        this.store = store;
        this.runs = runs;
        this.time = time;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static String jobName(Cadence cadence) {
        return "M10_" + cadence.name();
    }

    public void runDue(Cadence cadence) {
        List<M10TimedTask> due =
                tasks.orderedStream().filter(t -> t.cadence() == cadence).toList();
        if (due.isEmpty()) {
            return;
        }
        Instant actual = time.now();
        String runId = runs.start(jobName(cadence));
        int processed = 0;
        boolean failed = false;
        for (M10TimedTask task : due) {
            for (Instant scheduled : scheduledTimes(task, actual)) {
                try {
                    processed += runOnce(task, scheduled, actual, runId);
                } catch (RuntimeException e) {
                    failed = true;
                    log.warn("Việc {} của {} mốc {} lỗi", task.name(), jobName(cadence), scheduled, e);
                    // các đêm sau của tác vụ chờ lần chạy sau, giữ thứ tự
                    break;
                }
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }

    private List<Instant> scheduledTimes(M10TimedTask task, Instant actual) {
        return switch (task.cadence()) {
            case MINUTE -> List.of(actual.truncatedTo(ChronoUnit.MINUTES));
            case HOURLY -> List.of(actual.truncatedTo(ChronoUnit.HOURS));
            case MIDNIGHT -> {
                LocalDate today = time.today();
                LocalDate next = store.lastRun(task.name())
                        .map(last ->
                                last.atZone(BusinessTime.ZONE).toLocalDate().plusDays(1))
                        .orElse(today);
                List<Instant> nights = new ArrayList<>();
                for (LocalDate day = next; !day.isAfter(today); day = day.plusDays(1)) {
                    nights.add(time.startOfDay(day));
                }
                yield nights;
            }
        };
    }

    private int runOnce(M10TimedTask task, Instant scheduled, Instant actual, String runId) {
        Integer done = transaction.execute(status -> {
            if (!store.claim(task.name(), scheduled, runId)) {
                return 0;
            }
            return task.run(scheduled, actual);
        });
        return done == null ? 0 : done;
    }
}
