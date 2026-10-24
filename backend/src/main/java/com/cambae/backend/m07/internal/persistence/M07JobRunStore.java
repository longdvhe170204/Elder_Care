package com.cambae.backend.m07.internal.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Job run tracking store for Module M07 (table {@code m07_job_run}; research D-19).
 * Ensures each job and checkpoint runs at most once, including catch-up runs.
 */
@Repository
public class M07JobRunStore {

    private final JdbcTemplate jdbc;

    public M07JobRunStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Check whether a job at a specific checkpoint has completed.
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
     * Record completion of a job at a specific checkpoint.
     */
    public void recordCompletion(
            String jobKey, String checkpoint, String result, Long schedulerRunId, Instant completedAt) {
        jdbc.update(
                "INSERT INTO m07_job_run (job_key, checkpoint, result, scheduler_run_id, completed_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE result = VALUES(result), scheduler_run_id = VALUES(scheduler_run_id), completed_at = VALUES(completed_at)",
                jobKey,
                checkpoint,
                result,
                schedulerRunId,
                completedAt != null
                        ? LocalDateTime.ofInstant(completedAt, ZoneOffset.UTC)
                        : LocalDateTime.now(ZoneOffset.UTC));
    }
}
