package com.cambae.backend.common.scheduling;

import com.cambae.backend.common.time.BusinessTime;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

/**
 * Ghi lần chạy của job vào {@code scheduler_run}; id lần chạy dùng làm {@code schedulerRunId} trong căn cứ nhật ký
 * (FR-045). Thời điểm lấy từ đồng hồ của ứng dụng.
 */
@Component
public class SchedulerRuns {

    public enum Result {
        SUCCEEDED,
        FAILED
    }

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    public SchedulerRuns(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    /** Bắt đầu một lần chạy, trả id lần chạy. */
    public long start(String job) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO scheduler_run (job, started_at) VALUES (?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, job);
                    ps.setObject(2, now());
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    /** Kết thúc lần chạy với kết quả và số bản ghi đã xử lý. */
    public void finish(long runId, Result result, int processedCount) {
        jdbc.update(
                "UPDATE scheduler_run SET finished_at = ?, result = ?, processed_count = ? WHERE id = ?",
                now(),
                result.name(),
                processedCount,
                runId);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
    }
}
