package com.eldercare.backend.care.internal.session.persistence;

import com.eldercare.backend.care.internal.session.domain.TracedAttendance;
import com.eldercare.backend.care.internal.session.domain.TracedSession;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 007 FR-060 (b): chỉ đọc buổi, chuyến chồng khoảng và bản gốc điểm danh của chúng; giá trị hiện hành áp ở service. */
@Repository
public class AttendanceTraceStore {
    private final JdbcTemplate jdbc;

    public AttendanceTraceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Buổi, chuyến chưa hủy mà người cao tuổi có đăng ký, chồng khoảng [from, to): buổi trong viện theo giờ bắt đầu, kết
     * thúc; chuyến từ giờ rời dự kiến tới lúc kết thúc điểm danh về, chưa kết thúc thì tới giờ về dự kiến hiện hành.
     */
    public List<TracedSession> sessions(long residentId, Instant from, Instant to) {
        return jdbc.query(
                "SELECT x.id,x.f,x.t FROM (SELECT s.id,COALESCE(s.trip_planned_departure_at,s.starts_at) f,"
                        + "CASE WHEN s.trip_status IS NULL THEN s.ends_at"
                        + " ELSE COALESCE(s.trip_return_finished_at,s.trip_planned_return_at) END t"
                        + " FROM activity_session s WHERE s.status<>'CANCELLED'"
                        + " AND (s.trip_status IS NULL OR s.trip_status<>'CANCELLED')"
                        + " AND EXISTS (SELECT 1 FROM activity_registration r WHERE r.session_id=s.id"
                        + " AND r.resident_id=?)) x WHERE x.f<? AND x.t>? ORDER BY x.f,x.id",
                (rs, i) -> new TracedSession(
                        rs.getLong("id"),
                        rs.getObject("f", LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        rs.getObject("t", LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant()),
                residentId,
                LocalDateTime.ofInstant(to, BusinessTime.ZONE),
                LocalDateTime.ofInstant(from, BusinessTime.ZONE));
    }

    /** Bản gốc điểm danh buổi trong viện ({@code M04.SESSION_ATTENDANCE}) của các buổi. */
    public List<TracedAttendance> sessionAttendance(Collection<Long> sessionIds) {
        return rows(
                "M04.SESSION_ATTENDANCE",
                "JSON_EXTRACT(content,'$.sessionId')",
                "JSON_EXTRACT(content,'$.result')",
                sessionIds);
    }

    /** Bản gốc điểm danh chuyến ({@code M04.TRIP_ATTENDANCE}, mọi loại) của các chuyến. */
    public List<TracedAttendance> tripAttendance(Collection<Long> tripIds) {
        return rows(
                "M04.TRIP_ATTENDANCE", "JSON_EXTRACT(content,'$.tripId')", "JSON_EXTRACT(content,'$.type')", tripIds);
    }

    private List<TracedAttendance> rows(
            String kind, String sessionPath, String valuePath, Collection<Long> sessionIds) {
        if (sessionIds.isEmpty()) return List.of();
        var args = new ArrayList<Object>();
        args.add(kind);
        args.addAll(sessionIds);
        return jdbc.query(
                "SELECT id,CAST(" + sessionPath + " AS UNSIGNED) session_id,"
                        + "CAST(JSON_EXTRACT(content,'$.residentId') AS UNSIGNED) resident_id,"
                        + "JSON_UNQUOTE(" + valuePath + ") v FROM record_event WHERE kind=? AND CAST(" + sessionPath
                        + " AS UNSIGNED) IN (" + String.join(",", Collections.nCopies(sessionIds.size(), "?"))
                        + ")",
                (rs, i) -> new TracedAttendance(
                        rs.getLong("id"), rs.getLong("session_id"), rs.getLong("resident_id"), rs.getString("v")),
                args.toArray());
    }
}
