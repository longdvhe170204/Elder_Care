package com.eldercare.backend.admission.internal.change.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Dấu "xác nhận người đại diện bị bỏ" khi người duyệt Trả về nháp (FR-043e, Q-419) ở {@code reminder_marker}: marker là
 * thời điểm (mili giây, đệm 0 để so chuỗi). Xác nhận ghi trước dấu mới nhất hết tác dụng.
 */
@Repository
public class ConfirmationVoidStore {

    static final String KIND = "M02.CONFIRMATION_VOID";
    private static final String REQUEST_ENTITY = "M02.RESIDENCY_CHANGE";

    private final ReminderMarkers markers;
    private final JdbcTemplate jdbc;

    ConfirmationVoidStore(ReminderMarkers markers, JdbcTemplate jdbc) {
        this.markers = markers;
        this.jdbc = jdbc;
    }

    public void mark(long requestId, Instant at) {
        markers.mark(KIND, REQUEST_ENTITY, requestId, "%020d".formatted(at.toEpochMilli()), null);
    }

    public Optional<Instant> latest(long requestId) {
        String marker = jdbc.queryForObject(
                "SELECT MAX(marker) FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ?",
                String.class,
                KIND,
                REQUEST_ENTITY,
                requestId);
        return Optional.ofNullable(marker).map(m -> Instant.ofEpochMilli(Long.parseLong(m)));
    }
}
