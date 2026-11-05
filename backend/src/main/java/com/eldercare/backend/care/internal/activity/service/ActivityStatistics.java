package com.eldercare.backend.care.internal.activity.service;

import static com.eldercare.backend.care.internal.activity.persistence.ActivityStatisticsStore.instant;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.care.api.ActivityStatisticsApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.activity.persistence.ActivityStatisticsStore;
import com.eldercare.backend.care.internal.trip.domain.TripKinds;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T027 (contracts mục 1.2 {@code HoatDongThongKeApi}; FR-065, FR-070): số liệu chỉ đọc cho 016 theo phạm vi và khoảng
 * ngày (giờ nghiệp vụ, hai đầu tính cả). Điểm danh buổi trong viện theo giá trị hiện hành sau đính chính (000 FR-030);
 * chuyến tính Có mặt là người đã rời viện, chưa có Vắng theo lý do (FR-041 chưa ghi Vắng cho người không rời viện).
 * Nguy cơ cô lập (FR-058) trả rỗng tới khi có chuỗi cô lập (plan Phase 8).
 */
@Service
public class ActivityStatistics implements ActivityStatisticsApi {
    private static final String ATTENDANCE = "M04.SESSION_ATTENDANCE";

    private final ActivityStatisticsStore store;
    private final CorrectionApi corrections;
    private final RecordEventApi records;

    public ActivityStatistics(ActivityStatisticsStore store, CorrectionApi corrections, RecordEventApi records) {
        this.store = store;
        this.corrections = corrections;
        this.records = records;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<SessionStatisticsView> sessions(ScopeFilter scope, LocalDate from, LocalDate to) {
        var rows = store.sessions(scope, from, to);
        var ids = rows.stream().map(r -> ((Number) r.get("id")).longValue()).toList();
        var attendance = store.attendance(ids);
        var current = corrections.current(
                ATTENDANCE,
                attendance.stream().map(a -> ((Number) a.get("id")).longValue()).toList());
        var bySession = new HashMap<Long, List<String[]>>();
        for (var a : attendance) {
            long id = ((Number) a.get("id")).longValue();
            String result = (String) a.get("result");
            String reason = (String) a.get("absence_reason");
            var value = current.get(id);
            if (value != null && value.voided()) continue;
            if (value != null && value.corrected() && value.content() instanceof Map<?, ?> map) {
                result = text(map.get("result"));
                reason = text(map.get("absenceReason"));
            }
            bySession
                    .computeIfAbsent(((Number) a.get("session_id")).longValue(), k -> new ArrayList<>())
                    .add(new String[] {result, reason});
        }
        var views = new ArrayList<SessionStatisticsView>();
        for (var r : rows) {
            long id = ((Number) r.get("id")).longValue();
            boolean trip = r.get("trip_status") != null;
            int present = 0, notRecorded = 0;
            var absent = new TreeMap<String, Integer>();
            if (trip) present = ((Number) r.get("departed")).intValue();
            else
                for (var a : bySession.getOrDefault(id, List.of())) {
                    if ("PRESENT".equals(a[0])) present++;
                    else if ("NOT_RECORDED".equals(a[0])) notRecorded++;
                    // Vắng luôn kèm lý do (FR-028); không suy lý do khi thiếu.
                    else if ("ABSENT".equals(a[0]) && a[1] != null && !"null".equals(a[1]))
                        absent.merge(a[1], 1, Integer::sum);
                }
            views.add(new SessionStatisticsView(
                    id,
                    ((Number) r.get("activity_id")).longValue(),
                    Optional.ofNullable((Number) r.get("floor_area_id")).map(Number::longValue),
                    instant(r.get("starts_at")),
                    trip ? (String) r.get("trip_status") : (String) r.get("status"),
                    trip,
                    ((Number) r.get("registered")).intValue(),
                    present,
                    Map.copyOf(absent),
                    notRecorded));
        }
        return views.stream();
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<TripStatisticsView> trips(ScopeFilter scope, LocalDate from, LocalDate to) {
        var views = new ArrayList<TripStatisticsView>();
        for (var r : store.trips(scope, from, to)) {
            long id = ((Number) r.get("id")).longValue();
            var extensions = records.list(TripKinds.EXTENSIONS, new EntityRef("M04.ACTIVITY_SESSION", id));
            Instant planned = extensions.isEmpty()
                    ? instant(r.get("trip_planned_return_at"))
                    : extensions.getFirst().content().previousReturnAt();
            views.add(new TripStatisticsView(
                    id,
                    Optional.ofNullable((Number) r.get("floor_area_id")).map(Number::longValue),
                    planned,
                    extensions.stream().map(e -> e.content().newReturnAt()).toList(),
                    Optional.ofNullable(instant(r.get("trip_return_finished_at"))),
                    ((Number) r.get("missing")).intValue()));
        }
        return views.stream();
    }

    /** FR-065: chỉ mục đã đóng (Đạt, Không đạt, Không kiểm tra được, Quá hạn kiểm tra). */
    @Override
    @Transactional(readOnly = true)
    public Stream<QualityCheckStatisticsView> qualityChecks(ScopeFilter scope, LocalDate from, LocalDate to) {
        return store.qualityChecks(scope, from, to).stream()
                .map(r -> new QualityCheckStatisticsView(
                        ((Number) r.get("id")).longValue(),
                        ((Number) r.get("shift_id")).longValue(),
                        ((Number) r.get("floor_id")).longValue(),
                        ((Number) r.get("task_id")).longValue(),
                        (String) r.get("status"),
                        "null".equals(r.get("role")) ? null : (String) r.get("role"),
                        r.get("recorded_at") == null || "null".equals(r.get("recorded_at"))
                                ? null
                                : Instant.parse((String) r.get("recorded_at"))));
    }

    @Override
    public Stream<IsolationRiskView> isolationRisks() {
        return Stream.empty();
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
