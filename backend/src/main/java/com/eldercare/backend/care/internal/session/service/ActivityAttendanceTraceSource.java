package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.care.api.dto.ActivityAttendanceTraceView;
import com.eldercare.backend.care.api.spi.AttendanceTracePort;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.session.domain.TracedAttendance;
import com.eldercare.backend.care.internal.session.domain.TracedSession;
import com.eldercare.backend.care.internal.session.persistence.AttendanceTraceStore;
import com.eldercare.backend.care.internal.trip.domain.TripKinds;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DiemDanhTruyVetPort (007 FR-060 (b); 014 T014): các buổi chồng khoảng mà người cao tuổi Có mặt, kèm những người cùng
 * Có mặt. Buổi trong viện theo kết quả điểm danh hiện hành (sau đính chính, Hủy ghi nhận thì bỏ); chuyến theo điểm danh
 * rời viện chưa bị Hủy ghi nhận. Chỉ đọc; 007 lọc quyền xem ở lệnh tra cứu của mình.
 */
@Service
public class ActivityAttendanceTraceSource implements AttendanceTracePort {
    private static final String DEPARTURE = "DEPARTURE";

    private final AttendanceTraceStore store;
    private final CorrectionApi corrections;

    public ActivityAttendanceTraceSource(AttendanceTraceStore store, CorrectionApi corrections) {
        this.store = store;
        this.corrections = corrections;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityAttendanceTraceView> coAttendance(long residentId, Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) return List.of();
        var sessions = store.sessions(residentId, from, to);
        if (sessions.isEmpty()) return List.of();
        var ids = sessions.stream().map(TracedSession::id).toList();
        var present = new HashMap<Long, Set<Long>>();
        var attendance = store.sessionAttendance(ids);
        var current = corrections.current(
                SessionKinds.ATTENDANCE.code(),
                attendance.stream().map(TracedAttendance::id).toList());
        for (var row : attendance) {
            var value = current.get(row.id());
            if (value != null && value.voided()) continue;
            String result = row.value();
            if (value != null && value.corrected() && value.content() instanceof Map<?, ?> map)
                result = map.get("result") == null ? null : map.get("result").toString();
            if (SessionKinds.PRESENT.equals(result))
                present.computeIfAbsent(row.sessionId(), k -> new LinkedHashSet<>())
                        .add(row.residentId());
        }
        var departures = store.tripAttendance(ids).stream()
                .filter(row -> DEPARTURE.equals(row.value()))
                .toList();
        var tripCurrent = corrections.current(
                TripKinds.ATTENDANCE.code(),
                departures.stream().map(TracedAttendance::id).toList());
        for (var row : departures) {
            var value = tripCurrent.get(row.id());
            if (value != null && value.voided()) continue;
            present.computeIfAbsent(row.sessionId(), k -> new LinkedHashSet<>()).add(row.residentId());
        }
        var result = new ArrayList<ActivityAttendanceTraceView>();
        for (var session : sessions) {
            var people = present.getOrDefault(session.id(), Set.of());
            if (!people.contains(residentId)) continue;
            var others = people.stream().filter(id -> id != residentId).toList();
            result.add(new ActivityAttendanceTraceView(session.id(), session.from(), session.to(), others));
        }
        return result;
    }
}
