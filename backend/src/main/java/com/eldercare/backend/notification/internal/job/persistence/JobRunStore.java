package com.eldercare.backend.notification.internal.job.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Ngày đã chạy xong của job hằng ngày M13 ({@code lan_chay_m13}). */
@Repository
public class JobRunStore {

    private final JdbcTemplate jdbc;

    public JobRunStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean completed(String job, LocalDate day) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM m13_job_run WHERE job = ? AND business_date = ?", Integer.class, job, day);
        return n != null && n > 0;
    }

    public void markCompleted(String job, LocalDate day, long schedulerRunId, Instant at) {
        jdbc.update(
                "INSERT INTO m13_job_run (job, business_date, scheduler_run_id, completed_at) VALUES (?, ?, ?, ?)",
                job,
                day,
                schedulerRunId,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
    }
}
