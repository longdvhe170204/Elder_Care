package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chạy bù job hằng ngày của M02 (research T-11; FR-077, BR-M02-19). Xử lý từng ngày từ ngày kế tiếp ngày đã xong gần
 * nhất của mỗi việc tới hôm nay, theo thứ tự ngày; dấu {@code M02.DAILY_RUN} của {@code reminder_marker} duy nhất nên mỗi
 * việc mỗi ngày chạy đúng một lần, kể cả khi hai tiến trình cùng chạy. Lần đầu (chưa có lịch sử) chỉ xử lý ngày mới
 * nhất. Hôm nay chỉ được xử lý từ 00:15, sau job áp dụng yêu cầu 00:05 của 000, để việc hợp đồng dùng nội dung đã
 * áp dụng. Việc lỗi không chặn việc khác; ngày lỗi và các ngày sau của việc đó làm lại ở lần chạy sau.
 */
@Service
public class M02DailyRunner {

    public static final String JOB = "M02_DAILY";
    static final LocalTime RUN_AT = LocalTime.of(0, 15);

    private static final Logger log = LoggerFactory.getLogger(M02DailyRunner.class);

    private final ObjectProvider<M02DailyTask> tasks;
    private final SchedulerRuns runs;
    private final M02RunStore store;
    private final BusinessTime time;
    private final TransactionTemplate transaction;

    M02DailyRunner(
            ObjectProvider<M02DailyTask> tasks,
            SchedulerRuns runs,
            M02RunStore store,
            BusinessTime time,
            PlatformTransactionManager transactions) {
        this.tasks = tasks;
        this.runs = runs;
        this.store = store;
        this.time = time;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Chạy mọi (việc, ngày) còn thiếu tới ngày mới nhất được phép; không có gì thiếu thì không ghi lần chạy. */
    public void catchUp() {
        List<M02DailyTask> all = tasks.orderedStream().toList();
        if (all.isEmpty()) {
            return;
        }
        LocalDate latest = latestDay();
        List<Pending> pending = new ArrayList<>();
        for (M02DailyTask task : all) {
            LocalDate next = nextDay(task, latest);
            if (!next.isAfter(latest)) {
                pending.add(new Pending(task, next));
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        LocalDate first =
                pending.stream().map(Pending::from).min(LocalDate::compareTo).orElseThrow();
        String runId = runs.start(JOB);
        int processed = 0;
        boolean failed = false;
        Set<String> blocked = new HashSet<>();
        for (LocalDate day = first; !day.isAfter(latest); day = day.plusDays(1)) {
            for (Pending item : pending) {
                String name = item.task().name();
                if (day.isBefore(item.from()) || blocked.contains(name)) {
                    continue;
                }
                try {
                    processed += runDay(item.task(), day, day.equals(latest), runId);
                } catch (RuntimeException e) {
                    failed = true;
                    blocked.add(name);
                    log.warn("Việc {} của {} ngày {} lỗi", name, JOB, day, e);
                }
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }

    /** Ngày mới nhất được xử lý: hôm nay từ 00:15, trước đó là hôm qua. */
    private LocalDate latestDay() {
        LocalDate today = time.today();
        Instant dueAt = today.atTime(RUN_AT).atZone(BusinessTime.ZONE).toInstant();
        return time.now().isBefore(dueAt) ? today.minusDays(1) : today;
    }

    private LocalDate nextDay(M02DailyTask task, LocalDate latest) {
        return store.lastDay(task.name()).map(last -> last.plusDays(1)).orElse(latest);
    }

    /** Một (việc, ngày) trong transaction riêng: giành dấu ngày trước, chạy việc sau. */
    private int runDay(M02DailyTask task, LocalDate day, boolean isLatest, String runId) {
        Integer done = transaction.execute(status -> {
            boolean skipped = task.latestDayOnly() && !isLatest;
            boolean claimed = store.claimDay(task.name(), day, skipped ? "SKIPPED" : "SUCCEEDED", runId);
            if (!claimed || skipped) {
                return 0;
            }
            return task.run(day);
        });
        return done == null ? 0 : done;
    }

    private record Pending(M02DailyTask task, LocalDate from) {}
}
