package com.eldercare.backend.common.scheduling;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Dọn dữ liệu kỹ thuật (audit cơ sở dữ liệu F-11, F-12; data-model 000 mục 6): khóa chống lặp {@code idempotency_key} đã
 * qua {@code expires_at}, sự kiện {@code EVENT_PUBLICATION} đã giao xong quá {@link #KEEP_COMPLETED_EVENTS}. Không phải
 * bản ghi nghiệp vụ (000 FR-010a); sự kiện chưa giao xong giữ để giao lại khi khởi động (R-09).
 */
@Service
public class TechnicalDataCleanup {

    public static final String JOB = "CLEAN_TECHNICAL_DATA";
    /** Giữ sự kiện đã giao xong để tra cứu sự cố (thông số kỹ thuật, không phải CFG). */
    static final Duration KEEP_COMPLETED_EVENTS = Duration.ofDays(7);

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final SchedulerRuns runs;

    TechnicalDataCleanup(JdbcTemplate jdbc, BusinessTime time, SchedulerRuns runs) {
        this.jdbc = jdbc;
        this.time = time;
        this.runs = runs;
    }

    /** Chạy một lượt, trả mã lần chạy (UUID). */
    public String run() {
        String runId = runs.start(JOB);
        Instant now = time.now();
        int removed = jdbc.update("DELETE FROM idempotency_key WHERE expires_at <= ?", utc(now));
        removed += jdbc.update(
                "DELETE FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NOT NULL AND COMPLETION_DATE < ?",
                utc(now.minus(KEEP_COMPLETED_EVENTS)));
        runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, removed);
        return runId;
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
