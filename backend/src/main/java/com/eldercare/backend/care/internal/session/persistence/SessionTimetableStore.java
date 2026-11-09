package com.eldercare.backend.care.internal.session.persistence;

import com.eldercare.backend.care.api.spi.TimetableSourcePort.TimetableItemView;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 014 T006: buổi, chuyến người cao tuổi còn đăng ký trong ngày, mã nguồn {@code ACTIVITY_SESSION} của schema
 * {@code TimetableItem} (api-005); chuyến ghi trạng thái chuyến. Buổi bị hủy kéo theo đăng ký bị hủy nên không hiện.
 */
@Repository
public class SessionTimetableStore {
    private final JdbcTemplate jdbc;

    public SessionTimetableStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TimetableItemView> registered(long residentId, LocalDate date) {
        return jdbc.query(
                "SELECT s.starts_at, JSON_UNQUOTE(JSON_EXTRACT(a.content,'$.name')),"
                        + " COALESCE(s.trip_status, s.status)"
                        + " FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " JOIN simple_record a ON a.id=s.activity_id AND a.kind='M04.ACTIVITY'"
                        + " WHERE r.resident_id=? AND r.status<>'CANCELLED' AND s.status<>'CANCELLED'"
                        + " AND s.starts_at>=? AND s.starts_at<? ORDER BY s.starts_at, s.id",
                (rs, i) -> new TimetableItemView(
                        rs.getObject(1, LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        "ACTIVITY_SESSION",
                        rs.getString(2),
                        rs.getString(3)),
                residentId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay());
    }
}
