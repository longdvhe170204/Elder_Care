package com.eldercare.backend.resident.internal.persistence;

import com.eldercare.backend.resident.internal.domain.DueReminder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bảng kỹ thuật {@code periodic_reminder} của các việc nhắc định kỳ trong M01 (research H-12): khóa chính
 * {@code (type, resident_id, milestone_id)} chống nhắc trùng. Chỉ nhắc hồ sơ ở bốn trạng thái còn lưu trú.
 */
@Repository
public class PeriodicReminderStore {

    private static final String DUE = "SELECT r.resident_id, r.milestone_id, r.reminder_count FROM periodic_reminder r"
            + " JOIN resident s ON s.id = r.resident_id"
            + " WHERE r.type = ? AND s.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
            + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED')"
            + " AND COALESCE(r.last_reminded_at, r.started_at) <= ?";

    private final JdbcTemplate jdbc;

    PeriodicReminderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Bắt đầu nhắc; gọi lại không thêm dòng. */
    public void start(String type, long residentId, long milestoneId, Instant at) {
        jdbc.update(
                "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at) VALUES (?, ?, ?, ?)",
                type,
                residentId,
                milestoneId,
                utc(at));
    }

    /** Thêm dòng đã nhắc lần đầu; {@code false} khi dòng đã có (hai lần chạy chồng nhau). */
    public boolean startReminded(String type, long residentId, long milestoneId, Instant at) {
        return jdbc.update(
                        "INSERT IGNORE INTO periodic_reminder (type, resident_id, milestone_id, started_at,"
                                + " last_reminded_at, reminder_count) VALUES (?, ?, ?, ?, ?, 1)",
                        type,
                        residentId,
                        milestoneId,
                        utc(at),
                        utc(at))
                == 1;
    }

    /** Các dòng có lần nhắc gần nhất (hoặc lúc bắt đầu) không muộn hơn {@code before}. */
    public List<DueReminder> due(String type, Instant before) {
        return jdbc.query(
                DUE, (rs, i) -> new DueReminder(rs.getLong(1), rs.getLong(2), rs.getInt(3)), type, utc(before));
    }

    /**
     * Như {@link #due} và hồ sơ chưa có bản ghi Hiệu lực trong {@code table}; tên bảng là hằng do service truyền vào, không
     * lấy từ dữ liệu người dùng.
     */
    public List<DueReminder> dueWithoutActive(String type, String table, Instant before) {
        return jdbc.query(
                DUE + " AND NOT EXISTS (SELECT 1 FROM " + table
                        + " x WHERE x.resident_id = r.resident_id AND x.status = 'ACTIVE')",
                (rs, i) -> new DueReminder(rs.getLong(1), rs.getLong(2), rs.getInt(3)),
                type,
                utc(before));
    }

    public void markReminded(String type, long residentId, long milestoneId, Instant at, int number) {
        jdbc.update(
                "UPDATE periodic_reminder SET last_reminded_at = ?, reminder_count = ? WHERE type = ? AND"
                        + " resident_id = ? AND milestone_id = ?",
                utc(at),
                number,
                type,
                residentId,
                milestoneId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
