package com.cambae.backend.m09.internal.persistence;

import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Khóa theo (job,mốc), lưu thời điểm dự kiến/thực tế và kết quả; không lưu dữ liệu nghiệp vụ. */
@Repository
public class M09JobRuns {
    private final JdbcTemplate jdbc;

    public M09JobRuns(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    public boolean claim(String job, Instant scheduledAt, Instant actualAt) {
        jdbc.update(
                "INSERT IGNORE INTO m09_job_run(job,scheduled_at,started_at) VALUES(?,?,?)",
                job,
                utc(scheduledAt),
                utc(actualAt));
        String result = jdbc.queryForObject(
                "SELECT result FROM m09_job_run WHERE job=? AND scheduled_at=? FOR UPDATE",
                String.class,
                job,
                utc(scheduledAt));
        if ("SUCCEEDED".equals(result)) return false;
        jdbc.update(
                "UPDATE m09_job_run SET started_at=?,finished_at=NULL,result=NULL WHERE job=? AND scheduled_at=?",
                utc(actualAt),
                job,
                utc(scheduledAt));
        return true;
    }

    public void succeed(String job, Instant scheduledAt, Instant finishedAt) {
        jdbc.update(
                "UPDATE m09_job_run SET result='SUCCEEDED',finished_at=? WHERE job=? AND scheduled_at=?",
                utc(finishedAt),
                job,
                utc(scheduledAt));
    }

    public void fail(String job, Instant scheduledAt, Instant actualAt, Instant finishedAt) {
        jdbc.update(
                "INSERT INTO m09_job_run(job,scheduled_at,started_at,finished_at,result) VALUES(?,?,?,?,'FAILED') ON DUPLICATE KEY UPDATE finished_at=IF(result='SUCCEEDED',finished_at,VALUES(finished_at)),result=IF(result='SUCCEEDED',result,'FAILED')",
                job,
                utc(scheduledAt),
                utc(actualAt),
                utc(finishedAt));
    }
}
