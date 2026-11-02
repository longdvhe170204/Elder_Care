package com.eldercare.backend.admission.internal.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Khóa theo người và dấu chạy của job hằng ngày, dấu đã nhắc (data-model 004 mục 0; R-15): khóa trên dòng {@code
 * resident_module_state} của M02; mỗi (việc, ngày) một dấu {@code M02.DAILY_RUN} ở {@code reminder_marker} (BR-M02-19); mỗi
 * (loại nhắc, đối tượng, ngày) một dấu {@code M02.REMINDER}.
 */
@Repository
public class M02RunStore {

    public static final String DAILY_RUN = "M02.DAILY_RUN";
    public static final String REMINDER = "M02.REMINDER";
    private static final long NO_ENTITY = 0L;

    private final JdbcTemplate jdbc;
    private final ReminderMarkers markers;

    M02RunStore(JdbcTemplate jdbc, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.markers = markers;
    }

    /**
     * Khóa theo người tới hết transaction. {@code ON DUPLICATE KEY UPDATE} lấy khóa X ngay trên dòng sẵn có; {@code INSERT IGNORE}
     * chỉ lấy khóa S nên hai lệnh đồng thời cùng nâng lên X sẽ bế tắc (research T-03, phát hiện ở EndingConcurrencyIT).
     */
    public void lockResident(long residentId) {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M02', '{}', 0, UTC_TIMESTAMP(6))"
                        + " ON DUPLICATE KEY UPDATE resident_id = resident_id",
                residentId);
        jdbc.queryForObject(
                "SELECT resident_id FROM resident_module_state WHERE resident_id = ? AND module = 'M02' FOR UPDATE",
                Long.class,
                residentId);
    }

    /** Giành (việc, ngày); {@code false} khi đã có người chạy. */
    public boolean claimDay(String task, LocalDate day, String result, String runId) {
        return markers.mark(DAILY_RUN, task, NO_ENTITY, day.toString(), Map.of("result", result, "runId", runId));
    }

    /** Ngày đã chạy gần nhất của việc; mốc lưu dạng ISO nên so chuỗi đúng thứ tự ngày. */
    public Optional<LocalDate> lastDay(String task) {
        String last = jdbc.queryForObject(
                "SELECT MAX(marker) FROM reminder_marker WHERE kind = ? AND entity_type = ?",
                String.class,
                DAILY_RUN,
                task);
        return Optional.ofNullable(last).map(LocalDate::parse);
    }

    /** Ghi dấu đã nhắc của ngày; {@code false} khi ngày đó đã nhắc. */
    public boolean markReminded(String type, long subjectId, LocalDate day) {
        return markers.mark(REMINDER, type, subjectId, day.toString(), null);
    }

    /** Ngày nhắc gần nhất của (loại, đối tượng). */
    public Optional<LocalDate> lastReminded(String type, long subjectId) {
        String last = jdbc.queryForObject(
                "SELECT MAX(marker) FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ?",
                String.class,
                REMINDER,
                type,
                subjectId);
        return Optional.ofNullable(last).map(LocalDate::parse);
    }
}
