package com.eldercare.backend.care.internal.wellbeing.persistence;

import com.eldercare.backend.care.internal.wellbeing.domain.WellbeingSession;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ đọc buổi của người cao tuổi trong khoảng được yêu cầu. */
@Repository
public class WellbeingStore {
    private final JdbcTemplate jdbc;

    public WellbeingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Long, WellbeingSession> sessions(long residentId, Instant from, Instant to) {
        var rows = jdbc.query(
                "SELECT DISTINCT s.id,s.starts_at,JSON_EXTRACT(a.content,'$.groupActivity') = TRUE AS is_group"
                        + " FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " JOIN simple_record a ON a.id=s.activity_id AND a.kind='M04.ACTIVITY'"
                        + " WHERE r.resident_id=? AND s.starts_at>=? AND s.starts_at<? AND s.status<>'CANCELLED'"
                        + " ORDER BY s.starts_at,s.id",
                (rs, i) -> new WellbeingSession(
                        rs.getLong("id"),
                        rs.getObject("starts_at", LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        rs.getBoolean("is_group")),
                residentId,
                LocalDateTime.ofInstant(from, BusinessTime.ZONE),
                LocalDateTime.ofInstant(to, BusinessTime.ZONE));
        var result = new LinkedHashMap<Long, WellbeingSession>();
        rows.forEach(row -> result.put(row.id(), row));
        return result;
    }
}
