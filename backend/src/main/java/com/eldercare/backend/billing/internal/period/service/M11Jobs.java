package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import com.eldercare.backend.billing.internal.period.persistence.CostJobMarkers;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** T007/P-15: mốc đầu giữ riêng, từng lượt thành công cùng transaction với tác vụ. */
@Service
public class M11Jobs {
    private static final Logger log = LoggerFactory.getLogger(M11Jobs.class);
    private final ObjectProvider<CostTimedTask> tasks;
    private final CostJobMarkers history;
    private final ReminderMarkers markers;
    private final SchedulerRuns runs;
    private final BusinessTime time;
    private final TransactionTemplate transaction;

    public M11Jobs(
            ObjectProvider<CostTimedTask> tasks,
            CostJobMarkers history,
            ReminderMarkers markers,
            SchedulerRuns runs,
            BusinessTime time,
            PlatformTransactionManager manager) {
        this.tasks = tasks;
        this.history = history;
        this.markers = markers;
        this.runs = runs;
        this.time = time;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void runDue(CostJobCadence cadence) {
        var selected =
                tasks.orderedStream().filter(task -> task.cadence() == cadence).toList();
        if (selected.isEmpty()) return;
        var names = new HashSet<String>();
        for (var task : selected)
            if (!names.add(task.name())) throw new IllegalStateException("Trùng tên tác vụ M11: " + task.name());
        Instant latest = cadence.latest(time.now(), BusinessTime.ZONE);
        String previousMdc = MDC.get(SchedulerRuns.MDC_KEY);
        String runId = runs.start("M11_" + cadence.name());
        int processed = 0;
        boolean failed = false;
        try {
            for (var task : selected) {
                transaction.executeWithoutResult(status -> markers.mark(
                        "M11.JOB_ANCHOR",
                        CostJobMarkers.entityType(task.name()),
                        cadence.markerId(),
                        "FIRST",
                        Map.of("slot", latest.toString())));
                Instant slot = history.completed(task.name(), cadence)
                        .map(last -> cadence.next(last, BusinessTime.ZONE))
                        .orElseGet(() -> history.first(task.name(), cadence));
                for (; !slot.isAfter(latest); slot = cadence.next(slot, BusinessTime.ZONE)) {
                    Instant due = slot;
                    boolean skipped = task.latestOnly() && due.isBefore(latest);
                    try {
                        Integer count = transaction.execute(status -> {
                            if (!markers.mark(
                                    "M11.JOB_DONE",
                                    CostJobMarkers.entityType(task.name()),
                                    cadence.markerId(),
                                    due.toString(),
                                    Map.of("jobRunId", runId, "skipped", skipped))) return 0;
                            return skipped ? 0 : task.run(due);
                        });
                        processed += count == null ? 0 : count;
                    } catch (RuntimeException exception) {
                        failed = true;
                        log.warn("Tác vụ M11 {} tại {} lỗi, giữ để chạy lại", task.name(), due, exception);
                        break;
                    }
                }
            }
        } catch (RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
            if (previousMdc == null) MDC.remove(SchedulerRuns.MDC_KEY);
            else MDC.put(SchedulerRuns.MDC_KEY, previousMdc);
        }
    }
}
