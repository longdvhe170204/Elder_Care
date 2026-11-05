package com.eldercare.backend.care.internal.activity.persistence;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 014 T027: truy vấn chỉ đọc cho {@code ActivityStatisticsApi}; phạm vi chuyển thành {@code IN (...)} như 005. */
@Repository
public class ActivityStatisticsStore {
    private final JdbcTemplate jdbc;

    public ActivityStatisticsStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private static LocalDateTime utc(LocalDate date) {
        return LocalDateTime.ofInstant(date.atStartOfDay(BusinessTime.ZONE).toInstant(), ZoneOffset.UTC);
    }

    /** Điều kiện phạm vi theo tầng (cột {@code floorColumn}) hoặc người cao tuổi có đăng ký; {@code null} là không có dòng. */
    private static String scope(ScopeFilter scope, String floorColumn, String sessionColumn, List<Object> args) {
        if (scope.facilityWide()) return "";
        var terms = new ArrayList<String>();
        if (!scope.floorAreaIds().isEmpty()) {
            terms.add(floorColumn + " IN ("
                    + String.join(",", Collections.nCopies(scope.floorAreaIds().size(), "?")) + ")");
            args.addAll(scope.floorAreaIds());
        }
        if (sessionColumn != null && !scope.residentIds().isEmpty()) {
            terms.add("EXISTS (SELECT 1 FROM activity_registration x WHERE x.session_id=" + sessionColumn
                    + " AND x.resident_id IN ("
                    + String.join(",", Collections.nCopies(scope.residentIds().size(), "?")) + "))");
            args.addAll(scope.residentIds());
        }
        if (terms.isEmpty()) return null;
        return " AND (" + String.join(" OR ", terms) + ")";
    }

    public List<Map<String, Object>> sessions(ScopeFilter filter, LocalDate from, LocalDate to) {
        var args = new ArrayList<Object>(List.of(start(from), start(to.plusDays(1))));
        String where = scope(filter, "s.floor_area_id", "s.id", args);
        if (where == null) return List.of();
        return jdbc.queryForList(
                "SELECT s.id,s.activity_id,s.floor_area_id,s.starts_at,s.status,s.trip_status,"
                        + "(SELECT COUNT(*) FROM activity_registration r WHERE r.session_id=s.id AND r.status<>'CANCELLED') registered,"
                        + "(SELECT COUNT(*) FROM activity_registration r WHERE r.session_id=s.id"
                        + " AND r.participant_status IN ('DEPARTED','RETURNED','MISSING_ON_RETURN','LEFT_GROUP')) departed"
                        + " FROM activity_session s WHERE s.starts_at>=? AND s.starts_at<?" + where
                        + " ORDER BY s.starts_at,s.id",
                args.toArray());
    }

    /** Điểm danh buổi trong viện của các buổi đã cho (bản gốc; giá trị hiện hành áp ở service). */
    public List<Map<String, Object>> attendance(Collection<Long> sessionIds) {
        if (sessionIds.isEmpty()) return List.of();
        return jdbc.queryForList(
                "SELECT id,CAST(JSON_EXTRACT(content,'$.sessionId') AS UNSIGNED) session_id,"
                        + "JSON_UNQUOTE(JSON_EXTRACT(content,'$.result')) result,"
                        + "JSON_UNQUOTE(JSON_EXTRACT(content,'$.absenceReason')) absence_reason"
                        + " FROM record_event WHERE kind='M04.SESSION_ATTENDANCE'"
                        + " AND CAST(JSON_EXTRACT(content,'$.sessionId') AS UNSIGNED) IN ("
                        + String.join(",", Collections.nCopies(sessionIds.size(), "?")) + ")",
                sessionIds.toArray());
    }

    public List<Map<String, Object>> trips(ScopeFilter filter, LocalDate from, LocalDate to) {
        var args = new ArrayList<Object>(List.of(start(from), start(to.plusDays(1))));
        String where = scope(filter, "s.floor_area_id", "s.id", args);
        if (where == null) return List.of();
        return jdbc.queryForList(
                "SELECT s.id,s.floor_area_id,s.trip_planned_return_at,s.trip_return_finished_at,"
                        + "(SELECT COUNT(*) FROM activity_registration r WHERE r.session_id=s.id AND r.missing_alert_requested) missing"
                        + " FROM activity_session s WHERE s.trip_status IS NOT NULL"
                        + " AND s.trip_planned_departure_at>=? AND s.trip_planned_departure_at<?" + where
                        + " ORDER BY s.trip_planned_departure_at,s.id",
                args.toArray());
    }

    public List<Map<String, Object>> qualityChecks(ScopeFilter filter, LocalDate from, LocalDate to) {
        // record_event lưu UTC (000 Rows): đổi mốc ngày nghiệp vụ sang UTC.
        var args = new ArrayList<Object>(List.of(utc(from), utc(to.plusDays(1))));
        String where = scope(filter, "floor_area_id", null, args);
        if (where == null) return List.of();
        return jdbc.queryForList(
                "SELECT id,CAST(JSON_EXTRACT(content,'$.shiftId') AS UNSIGNED) shift_id,"
                        + "CAST(JSON_EXTRACT(content,'$.floorAreaId') AS UNSIGNED) floor_id,"
                        + "CAST(JSON_EXTRACT(content,'$.taskId') AS UNSIGNED) task_id,"
                        + "JSON_UNQUOTE(JSON_EXTRACT(content,'$.status')) status,"
                        + "JSON_UNQUOTE(JSON_EXTRACT(content,'$.inspectorRole')) role,"
                        + "JSON_UNQUOTE(JSON_EXTRACT(content,'$.recordedAt')) recorded_at"
                        + " FROM record_event WHERE kind='M04.QUALITY_CHECK_ITEM' AND confirmed_at IS NOT NULL"
                        + " AND confirmed_at>=? AND confirmed_at<?" + where + " ORDER BY confirmed_at,id",
                args.toArray());
    }

    public static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime local)
            return local.atZone(BusinessTime.ZONE).toInstant();
        return ((java.sql.Timestamp) value)
                .toLocalDateTime()
                .atZone(BusinessTime.ZONE)
                .toInstant();
    }
}
