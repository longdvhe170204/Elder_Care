package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.ReminderCycle;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * T066 (R-15; data-model 003 mục 0.1): dấu nhắc phân bổ trên {@code reminder_marker} (thay {@code allocation_reminder}).
 * Loại {@code M03.ALLOCATION_REMINDER.<flagType>}, đối tượng (target_type, target_id). Mỗi lần đặt dấu là một chu kỳ c:
 * {@code START:c} lúc bắt đầu, {@code c:n} lần nhắc thứ n, {@code END:c} khi gỡ dấu; {@code details.at} giữ mốc nghiệp
 * vụ. Ghi qua {@link ReminderMarkers}; đọc bằng SQL dòng loại của M03 (R-15 ghi nhớ (7)). Lệnh gọi đã khóa nguồn của dấu.
 */
@Repository
public class AllocationReminderMarkers {
    static final String KIND = "M03.ALLOCATION_REMINDER.";
    static final List<String> FLAGS =
            List.of("WITHOUT_BED", "AWAITING_REQUEST", "AWAITING_SAME_PRICE", "ROOM_UNSUITABLE");

    private final JdbcTemplate jdbc;
    private final ReminderMarkers markers;

    public AllocationReminderMarkers(JdbcTemplate jdbc, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.markers = markers;
    }

    private static Map<String, String> at(Instant at) {
        return Map.of("at", at.toString());
    }

    private Instant markedAt(String kind, String type, long id, String marker) {
        return jdbc
                .query(
                        "SELECT COALESCE(JSON_UNQUOTE(details->'$.at'),"
                                + " DATE_FORMAT(marked_at,'%Y-%m-%dT%H:%i:%s.%fZ')) FROM reminder_marker"
                                + " WHERE kind=? AND entity_type=? AND entity_id=? AND marker=?",
                        (r, n) -> Instant.parse(r.getString(1)), kind, type, id, marker)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Chu kỳ đang mở (có START, chưa có END) của dấu. */
    public Optional<ReminderCycle> open(String type, long id, String flag) {
        String kind = KIND + flag;
        Integer cycle = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(marker,7) AS UNSIGNED)) FROM reminder_marker"
                        + " WHERE kind=? AND entity_type=? AND entity_id=? AND marker LIKE 'START:%'",
                Integer.class, kind, type, id);
        if (cycle == null || markers.isMarked(kind, type, id, "END:" + cycle)) return Optional.empty();
        int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reminder_marker WHERE kind=? AND entity_type=? AND entity_id=? AND marker LIKE ?",
                Integer.class,
                kind,
                type,
                id,
                cycle + ":%");
        return Optional.of(new ReminderCycle(
                cycle,
                markedAt(kind, type, id, "START:" + cycle),
                count == 0 ? null : markedAt(kind, type, id, cycle + ":" + count),
                count));
    }

    private int lastCycle(String kind, String type, long id) {
        Integer cycle = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(marker,7) AS UNSIGNED)) FROM reminder_marker"
                        + " WHERE kind=? AND entity_type=? AND entity_id=? AND marker LIKE 'START:%'",
                Integer.class, kind, type, id);
        return cycle == null ? 0 : cycle;
    }

    /** Mở chu kỳ nếu chưa có chu kỳ mở; trả chu kỳ đang mở. */
    public ReminderCycle start(String type, long id, String flag, Instant at) {
        var open = open(type, id, flag);
        if (open.isPresent()) return open.get();
        String kind = KIND + flag;
        int cycle = lastCycle(kind, type, id) + 1;
        markers.mark(kind, type, id, "START:" + cycle, at(at));
        return new ReminderCycle(cycle, at, null, 0);
    }

    /** Đặt lại dấu: đóng chu kỳ đang mở rồi mở chu kỳ mới tại {@code at} (số lần về 0). */
    public void restart(String type, long id, String flag, Instant at) {
        end(type, id, flag);
        start(type, id, flag, at);
    }

    /** Ghi một lần nhắc vào chu kỳ đang mở. */
    public void remind(String type, long id, String flag, Instant at) {
        open(type, id, flag)
                .ifPresent(c -> markers.mark(KIND + flag, type, id, c.cycle() + ":" + (c.count() + 1), at(at)));
    }

    public void end(String type, long id, String flag) {
        open(type, id, flag).ifPresent(c -> markers.mark(KIND + flag, type, id, "END:" + c.cycle(), null));
    }

    public void endAll(String type, long id) {
        FLAGS.forEach(flag -> end(type, id, flag));
    }

    /** Đối tượng có chu kỳ đang mở: (target_type, target_id, flagType). */
    public List<String[]> openKeys() {
        return jdbc.query(
                "SELECT s.entity_type,s.entity_id,SUBSTRING(s.kind,25) FROM reminder_marker s"
                        + " WHERE s.kind LIKE 'M03.ALLOCATION\\_REMINDER.%' AND s.marker LIKE 'START:%'"
                        + " AND NOT EXISTS (SELECT 1 FROM reminder_marker e WHERE e.kind=s.kind"
                        + " AND e.entity_type=s.entity_type AND e.entity_id=s.entity_id"
                        + " AND e.marker=CONCAT('END:',SUBSTRING(s.marker,7)))",
                (r, n) -> new String[] {r.getString(1), Long.toString(r.getLong(2)), r.getString(3)});
    }
}
