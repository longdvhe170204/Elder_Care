package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc trạng thái hiện tại của hồ sơ người cao tuổi (DBR-01: đúng một trạng thái); ghi mốc đã cảnh báo Tạm vắng quá hạn. */
@Repository
public class ResidentStatusStore {

    private static final DateTimeFormatter MARKER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private final JdbcTemplate jdbc;
    private final ReminderMarkers markers;

    ResidentStatusStore(JdbcTemplate jdbc, ReminderMarkers markers) {
        this.jdbc = jdbc;
        this.markers = markers;
    }

    public Optional<ResidentStatus> find(long residentId) {
        return jdbc
                .query(
                        "SELECT status FROM resident WHERE id = ?",
                        (rs, i) -> ResidentStatus.valueOf(rs.getString(1)),
                        residentId)
                .stream()
                .findFirst();
    }

    /** Mã các hồ sơ đang ở trạng thái cho trước. */
    public List<Long> idsByStatus(ResidentStatus status) {
        return jdbc.query("SELECT id FROM resident WHERE status = ?", (rs, i) -> rs.getLong(1), status.name());
    }

    /**
     * Ghi đã cảnh báo (lúc hiện tại của viện) cho mốc trở lại này: dấu {@code M01.RETURN_OVERDUE_ALERT} của lượt vắng, dấu
     * là mốc dự kiến trở lại; {@code false} khi mốc đã được cảnh báo (FR-050).
     */
    public boolean recordOverdueAlert(long absenceId, Instant expectedReturnAt, long residentId) {
        return markers.mark(
                "M01.RETURN_OVERDUE_ALERT",
                "M02.ABSENCE",
                absenceId,
                MARKER.format(LocalDateTime.ofInstant(expectedReturnAt, ZoneOffset.UTC)),
                Map.of("residentId", residentId));
    }
}
