package com.cambae.backend.m15.approval.internal;

import com.cambae.backend.common.scheduling.SchedulerRuns;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Một đợt áp dụng các yêu cầu Đã duyệt đến hạn (000 T093; BR-M15-10, FR-037, FR-037a, FR-037b): theo thứ tự
 * {@code (ngày hiệu lực thực tế, thời điểm quyết định)}, mỗi yêu cầu một transaction qua {@link RequestApplier}; lỗi ở
 * một yêu cầu không chặn các yêu cầu sau.
 */
@Service
public class DueRequestApplication {

    static final String JOB = "APPLY_DUE_REQUESTS";

    private final JdbcTemplate jdbc;
    private final RequestApplier applier;
    private final SchedulerRuns runs;
    private final BusinessTime time;

    DueRequestApplication(JdbcTemplate jdbc, RequestApplier applier, SchedulerRuns runs, BusinessTime time) {
        this.jdbc = jdbc;
        this.applier = applier;
        this.runs = runs;
        this.time = time;
    }

    /** Chạy một đợt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        long runId = runs.start(JOB);
        List<Long> due = jdbc.queryForList(
                "SELECT id FROM approval_request WHERE status = ? AND actual_effective_date <= ?"
                        + " ORDER BY actual_effective_date, decided_at, id",
                Long.class,
                ApprovalStatus.APPROVED,
                time.today());
        int processed = 0;
        for (long id : due) {
            if (applier.applySafely(id, runId)) {
                processed++;
            }
        }
        runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }
}
