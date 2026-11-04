package com.eldercare.backend.family.internal.scheduling.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Dấu đã chạy của khung job M10 ({@code lan_chay_m10} → {@code reminder_marker} loại {@code M10.JOB_RUN}, R-15): mỗi
 * (tác vụ, mốc) một dấu; mốc lưu dạng {@link Instant#toString()} của mốc đã cắt nên so chuỗi đúng thứ tự.
 */
@Repository
public class M10JobRunStore {

    static final String JOB_RUN = "M10.JOB_RUN";
    private static final long NO_ENTITY = 0L;
    private static final int MAX_TASK_NAME = 40;

    private final JdbcTemplate jdbc;
    private final ReminderMarkers markers;

    M10JobRunStore(JdbcTemplate jdbc, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.markers = markers;
    }

    /** Giành (tác vụ, mốc) trong transaction của lần chạy; {@code false} khi mốc đã chạy xong. */
    public boolean claim(String task, Instant scheduledAt, String runId) {
        // INSERT IGNORE cắt chuỗi quá cột entity_type mà không báo, lần sau không tìm thấy dấu
        if (task.length() > MAX_TASK_NAME) {
            throw new IllegalArgumentException("Tên tác vụ quá " + MAX_TASK_NAME + " ký tự: " + task);
        }
        return markers.mark(JOB_RUN, task, NO_ENTITY, scheduledAt.toString(), Map.of("runId", runId));
    }

    public Optional<Instant> lastRun(String task) {
        String last = jdbc.queryForObject(
                "SELECT MAX(marker) FROM reminder_marker WHERE kind = ? AND entity_type = ?",
                String.class,
                JOB_RUN,
                task);
        return Optional.ofNullable(last).map(Instant::parse);
    }
}
