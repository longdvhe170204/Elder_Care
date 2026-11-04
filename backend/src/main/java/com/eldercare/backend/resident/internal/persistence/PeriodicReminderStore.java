package com.eldercare.backend.resident.internal.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Các việc nhắc định kỳ trong M01 (research H-12) trên {@code reminder_marker} (data-model 001 mục 0.1): loại
 * {@code M01.PERIODIC_REMINDER.<type>}, đối tượng {@code M01.RESIDENT}. Dấu {@code START:<milestoneId>} ghi lúc bắt đầu,
 * {@code <milestoneId>:<lần>} ghi mỗi lần nhắc; số lần và lần nhắc cuối tính từ các dòng, khóa duy nhất của bảng chống
 * nhắc trùng. Chỉ nhắc hồ sơ ở bốn trạng thái còn lưu trú.
 */
@Repository
public class PeriodicReminderStore {

    private static final String KIND = "M01.PERIODIC_REMINDER.";
    private static final String ENTITY = "M01.RESIDENT";
    private static final String START = "START:";

    private static final String DUE_SELECT =
            "SELECT r.entity_id, CAST(SUBSTRING_INDEX(REPLACE(r.marker, 'START:', ''), ':', 1) AS UNSIGNED) AS milestone,"
                    + " SUM(r.marker NOT LIKE 'START:%') AS reminders FROM reminder_marker r"
                    + " JOIN resident s ON s.id = r.entity_id"
                    + " WHERE r.kind = ? AND r.entity_type = 'M01.RESIDENT' AND s.status IN ('IN_RESIDENCE',"
                    + " 'TEMPORARILY_ABSENT', 'OFF_SITE_ACTIVITY', 'HOSPITALIZED')";
    private static final String DUE_GROUP =
            " GROUP BY r.entity_id, milestone HAVING MAX(r.marked_at) <= ? ORDER BY r.entity_id, milestone";

    private final JdbcTemplate jdbc;
    private final ReminderMarkers markers;

    PeriodicReminderStore(JdbcTemplate jdbc, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.markers = markers;
    }

    /** Bắt đầu nhắc (lúc hiện tại của viện); gọi lại không thêm dòng. */
    public void start(String type, long residentId, long milestoneId) {
        markers.mark(KIND + type, ENTITY, residentId, START + milestoneId, null);
    }

    /** Bắt đầu và ghi luôn lần nhắc đầu; {@code false} khi việc nhắc đã bắt đầu (hai lần chạy chồng nhau). */
    public boolean startReminded(String type, long residentId, long milestoneId) {
        if (!markers.mark(KIND + type, ENTITY, residentId, START + milestoneId, null)) {
            return false;
        }
        markReminded(type, residentId, milestoneId, 1);
        return true;
    }

    /** Các việc nhắc có lần nhắc gần nhất (hoặc lúc bắt đầu) không muộn hơn {@code before}. */
    public List<DueReminder> due(String type, Instant before) {
        return query(DUE_SELECT + DUE_GROUP, type, before);
    }

    /**
     * Như {@link #due} và hồ sơ chưa có bản ghi Hiệu lực trong {@code table}; tên bảng là hằng do service truyền vào, không
     * lấy từ dữ liệu người dùng.
     */
    public List<DueReminder> dueWithoutActive(String type, String table, Instant before) {
        return query(
                DUE_SELECT + " AND NOT EXISTS (SELECT 1 FROM " + table
                        + " x WHERE x.resident_id = r.entity_id AND x.status = 'ACTIVE')" + DUE_GROUP,
                type,
                before);
    }

    /** Ghi lần nhắc thứ {@code number} (lúc hiện tại của viện). */
    public void markReminded(String type, long residentId, long milestoneId, int number) {
        markers.mark(KIND + type, ENTITY, residentId, milestoneId + ":" + number, null);
    }

    private List<DueReminder> query(String sql, String type, Instant before) {
        return jdbc.query(
                sql,
                (rs, i) -> new DueReminder(rs.getLong(1), rs.getLong(2), rs.getInt(3)),
                KIND + type,
                LocalDateTime.ofInstant(before, ZoneOffset.UTC));
    }
}
