package com.eldercare.backend.common.data;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dấu "đã nhắc", "đã cảnh báo", "đã xử lý" trên {@code reminder_marker} (kỹ thuật, chỉ thêm; research R-15,
 * module-interfaces 000 mục 1.8). Mỗi bộ {@code (kind, entityType, entityId, marker)} ghi đúng một lần.
 */
@Component
public class ReminderMarkers {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final BusinessTime time;

    public ReminderMarkers(JdbcTemplate jdbc, JsonMapper json, BusinessTime time) {
        this.jdbc = jdbc;
        this.json = json;
        this.time = time;
    }

    /** Ghi dấu; {@code true} khi vừa ghi, {@code false} khi đã có (lần nhắc này đã làm). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean mark(String kind, String entityType, long entityId, String marker, Object details) {
        return jdbc.update(
                        "INSERT IGNORE INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        kind,
                        entityType,
                        entityId,
                        marker,
                        LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                        details == null ? null : json.writeValueAsString(details))
                == 1;
    }

    public boolean isMarked(String kind, String entityType, long entityId, String marker) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ?"
                        + " AND marker = ?)",
                Boolean.class,
                kind,
                entityType,
                entityId,
                marker));
    }
}
