package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.scheduling.SchedulerRuns;
import com.cambae.backend.common.time.BusinessTime;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chạy job khai báo hằng ngày (research Z-06; NFR-04): mỗi việc xử lý ngày mới nhất được phép (hôm nay từ 06:00, trước đó
 * là hôm qua) nếu ngày đó chưa chạy. Việc của 018 xét trạng thái tại ngày chạy (quá hạn, sắp hết hạn, đã hết hạn, cần khai
 * báo xóa) nên ngày bị lỡ không chạy lại từng ngày: lần chạy đầu tiên sau đó, kể cả lần chạy bù lúc khởi động, xử lý đủ
 * (SC-002). Bảng {@code daily_run (task, date)} của M02 có khóa chính hai cột nên mỗi việc mỗi ngày chạy đúng một lần, kể
 * cả khi hai tiến trình cùng chạy. Việc lỗi không chặn việc khác và được làm lại ở lần chạy sau. Không lập khai báo (Q-306).
 */
@Service
public class RegistrationDailyRunner {

    public static final String JOB = "M02_REGISTRATION";
    static final LocalTime RUN_AT = LocalTime.of(6, 0);

    private static final Logger log = LoggerFactory.getLogger(RegistrationDailyRunner.class);

    private final ObjectProvider<RegistrationDailyTask> tasks;
    private final SchedulerRuns runs;
    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final TransactionTemplate transaction;

    RegistrationDailyRunner(
            ObjectProvider<RegistrationDailyTask> tasks,
            SchedulerRuns runs,
            JdbcTemplate jdbc,
            BusinessTime time,
            PlatformTransactionManager transactions) {
        this.tasks = tasks;
        this.runs = runs;
        this.jdbc = jdbc;
        this.time = time;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Chạy các việc chưa chạy của ngày mới nhất được phép; không có gì thiếu thì không ghi lần chạy. */
    public void catchUp() {
        LocalDate day = latestDay();
        List<RegistrationDailyTask> pending =
                tasks.orderedStream().filter(task -> !done(task, day)).toList();
        if (pending.isEmpty()) {
            return;
        }
        long runId = runs.start(JOB);
        int processed = 0;
        boolean failed = false;
        for (RegistrationDailyTask task : pending) {
            try {
                processed += runDay(task, day, runId);
            } catch (RuntimeException e) {
                failed = true;
                log.warn("Việc {} của {} ngày {} lỗi", task.name(), JOB, day, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
    }

    private LocalDate latestDay() {
        LocalDate today = time.today();
        Instant dueAt = today.atTime(RUN_AT).atZone(BusinessTime.ZONE).toInstant();
        return time.now().isBefore(dueAt) ? today.minusDays(1) : today;
    }

    private boolean done(RegistrationDailyTask task, LocalDate day) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM daily_run WHERE task = ? AND `date` = ?",
                        Integer.class,
                        task.name(),
                        Date.valueOf(day))
                > 0;
    }

    /** Một (việc, ngày) trong transaction riêng: giành dòng {@code daily_run} trước, chạy việc sau; lỗi thì bỏ cả hai. */
    private int runDay(RegistrationDailyTask task, LocalDate day, long runId) {
        Integer done = transaction.execute(status -> {
            int claimed = jdbc.update(
                    "INSERT IGNORE INTO daily_run (task, `date`, result, scheduler_run_id) VALUES (?, ?, 'SUCCEEDED', ?)",
                    task.name(),
                    Date.valueOf(day),
                    runId);
            return claimed == 0 ? 0 : task.run(day);
        });
        return done == null ? 0 : done;
    }
}
