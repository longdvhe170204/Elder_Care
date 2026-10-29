package com.eldercare.backend.administration.approval.internal.service;

import com.eldercare.backend.administration.approval.api.ApprovalStatus;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Một đợt nhắc yêu cầu Chờ duyệt quá hạn (000 T094; BR-M15-09, FR-031a). Mỗi yêu cầu xét trong transaction riêng
 * ({@link ReminderTransaction}); lỗi ở một yêu cầu không chặn các yêu cầu sau. Không bao giờ tự hủy yêu cầu.
 */
@Service
public class PendingRequestReminder {

    static final String JOB = "REMIND_PENDING_REQUESTS";

    private static final Logger LOG = LoggerFactory.getLogger(PendingRequestReminder.class);

    private final JdbcTemplate jdbc;
    private final ReminderTransaction transaction;
    private final SchedulerRuns runs;

    PendingRequestReminder(JdbcTemplate jdbc, ReminderTransaction transaction, SchedulerRuns runs) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.runs = runs;
    }

    /** Chạy một đợt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        long runId = runs.start(JOB);
        List<Long> pending = jdbc.queryForList(
                "SELECT id FROM approval_request WHERE status = ? AND escalated_at IS NULL ORDER BY submitted_at, id",
                Long.class,
                ApprovalStatus.PENDING_APPROVAL);
        int processed = 0;
        boolean failed = false;
        for (long id : pending) {
            try {
                if (transaction.remind(id, runId)) {
                    processed++;
                }
            } catch (RuntimeException e) {
                failed = true;
                LOG.warn("Nhắc yêu cầu {} lỗi", id, e);
            }
        }
        runs.finish(runId, failed ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }
}
