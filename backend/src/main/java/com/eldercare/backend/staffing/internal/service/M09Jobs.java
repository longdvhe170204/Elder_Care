package com.eldercare.backend.staffing.internal.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.service.M09TimedTask.Cadence;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T050: marker và tác động commit cùng transaction; thất bại rollback để retry, lần chạy ghi log UUID. */
@Service
public class M09Jobs {
    private final ReminderMarkers markers;
    private final SchedulerRuns runs;
    private final BusinessTime time;
    private final ObjectProvider<M09TimedTask> tasks;
    private final TransactionTemplate transaction;

    public M09Jobs(
            ReminderMarkers markers,
            SchedulerRuns runs,
            BusinessTime time,
            ObjectProvider<M09TimedTask> tasks,
            PlatformTransactionManager manager) {
        this.markers = markers;
        this.runs = runs;
        this.time = time;
        this.tasks = tasks;
        this.transaction = new TransactionTemplate(manager);
    }

    public void runDue(Cadence cadence) {
        Instant actual = time.now();
        Instant scheduled = scheduledAt(cadence, actual);
        String job = "M09_" + cadence.name();
        String previous = MDC.get(SchedulerRuns.MDC_KEY);
        String runId = runs.start(job);
        try {
            int processed = transaction.execute(status -> {
                if (!markers.mark(
                        "M09.JOB." + job,
                        "M09.JOB",
                        0,
                        scheduled.toString(),
                        Map.of("scheduledAt", scheduled, "actualAt", actual, "schedulerRunId", runId))) return 0;
                int count = 0;
                for (var task : tasks.orderedStream()
                        .filter(t -> t.cadence() == cadence)
                        .toList()) count += task.run(scheduled, actual);
                return count;
            });
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, processed);
        } catch (RuntimeException exception) {
            runs.finish(runId, SchedulerRuns.Result.FAILED, 0);
            throw exception;
        } finally {
            if (previous == null) MDC.remove(SchedulerRuns.MDC_KEY);
            else MDC.put(SchedulerRuns.MDC_KEY, previous);
        }
    }

    private Instant scheduledAt(Cadence cadence, Instant actual) {
        return switch (cadence) {
            case MINUTE -> actual.truncatedTo(ChronoUnit.MINUTES);
            case HOURLY -> actual.truncatedTo(ChronoUnit.HOURS);
            case DAILY -> {
                var local = actual.atZone(BusinessTime.ZONE);
                var due = local.toLocalDate().atTime(0, 10).atZone(BusinessTime.ZONE);
                yield (due.toInstant().isAfter(actual) ? due.minusDays(1) : due).toInstant();
            }
        };
    }
}
