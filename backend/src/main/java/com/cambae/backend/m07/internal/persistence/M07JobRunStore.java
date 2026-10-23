package com.cambae.backend.m07.internal.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Quản lý theo dõi lần chạy job M07 (bảng {@code m07_job_run}; research D-19).
 * Đảm bảo mỗi việc mỗi mốc chỉ chạy một lần, kể cả chạy bù.
 */
@Repository
public class M07JobRunStore {

    private final JdbcTemplate jdbc;

    public M07JobRunStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Kiểm tra một việc tại một mốc đã hoàn thành chưa.
     */
    public boolean isCompleted(String jobKey, String checkpoint) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM m07_job_run WHERE job_key = ? AND checkpoint = ?",
                Integer.class,
                jobKey,
                checkpoint);
        return count != null && count > 0;
    }

    /**
     * Ghi nhận việc tại một mốc đã hoàn thành.
     */
    public void recordCompletion(String jobKey, String checkpoint, String result, Long schedulerRunId, Instant completedAt) {
        jdbc.update(
                "INSERT INTO m07_job_run (job_key, checkpoint, result, scheduler_run_id, completed_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE result = VALUES(result), scheduler_run_id = VALUES(scheduler_run_id), completed_at = VALUES(completed_at)",
                jobKey,
                checkpoint,
                result,
                schedulerRunId,
                completedAt != null ? LocalDateTime.ofInstant(completedAt, ZoneOffset.UTC) : LocalDateTime.now(ZoneOffset.UTC));
    }
}
