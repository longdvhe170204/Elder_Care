package com.eldercare.backend.care.internal.dailyschedule.persistence;

import com.eldercare.backend.care.internal.dailyschedule.domain.FacilitySession;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 005 FR-055: buổi hoạt động mở cho toàn viện và giá trị đo của công việc đo chỉ số trong ngày; chỉ đọc. */
@Repository
public class DailyScheduleStore {
    private final JdbcTemplate jdbc;

    public DailyScheduleStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Buổi không gắn tầng (mở cho toàn viện, feature 014), chưa hủy. */
    public List<FacilitySession> facilitySessions(LocalDate date) {
        return jdbc.query(
                "SELECT s.starts_at, JSON_UNQUOTE(JSON_EXTRACT(a.content,'$.name')) FROM activity_session s"
                        + " JOIN simple_record a ON a.id=s.activity_id AND a.kind='M04.ACTIVITY'"
                        + " WHERE s.floor_area_id IS NULL AND s.status<>'CANCELLED' AND s.starts_at>=? AND s.starts_at<?"
                        + " ORDER BY s.starts_at, s.id",
                (rs, i) -> new FacilitySession(
                        rs.getObject(1, LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        rs.getString(2)),
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay());
    }

    /** Mã công việc → giá trị đo đã ghi. */
    public Map<Long, BigDecimal> measuredValues(long residentId, Instant from, Instant to) {
        var result = new HashMap<Long, BigDecimal>();
        jdbc.query(
                "SELECT id, result_numeric_value FROM care_task WHERE resident_id=? AND planned_at>=? AND planned_at<?"
                        + " AND result_type='MEASURED_VALUE' AND result_numeric_value IS NOT NULL",
                rs -> {
                    result.put(rs.getLong(1), rs.getBigDecimal(2));
                },
                residentId,
                LocalDateTime.ofInstant(from, BusinessTime.ZONE),
                LocalDateTime.ofInstant(to, BusinessTime.ZONE));
        return result;
    }
}
