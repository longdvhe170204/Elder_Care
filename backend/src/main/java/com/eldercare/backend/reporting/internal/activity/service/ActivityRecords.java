package com.eldercare.backend.reporting.internal.activity.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.ActivityStatisticsApi;
import com.eldercare.backend.care.api.dto.SessionStatisticsView;
import com.eldercare.backend.care.api.dto.TripStatisticsView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buổi hoạt động, lượt điểm danh, chuyến đi cho {@code ACT.*} (FR-020, FR-035) đọc {@code ActivityStatisticsApi} của 014:
 * số đăng ký, Có mặt, Vắng theo lý do, Không ghi nhận của từng buổi (đã tính đính chính, bỏ Hủy ghi nhận); giờ về dự kiến,
 * các lần gia hạn, lúc điểm danh về, số người thiếu của chuyến.
 */
@Component
@Transactional(readOnly = true)
public class ActivityRecords {

    /** Lý do Vắng vào mẫu số tỷ lệ tham gia (FR-035): từ chối, sức khỏe, khác. */
    static final Set<String> COUNTED_ABSENCES = Set.of("REFUSED", "HEALTH", "OTHER");

    static final String PRESENT = "PRESENT";
    static final String LATE_RECORDED = "LATE_RECORDED";
    static final String OVERDUE = "OVERDUE";
    static final String OVERDUE_DOUBLE = "OVERDUE_DOUBLE";

    /** Một buổi; mốc xếp khoảng là giờ bắt đầu. */
    public record SessionRecord(SessionStatisticsView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.ACTIVITY_SESSION:" + view.sessionId();
        }

        @Override
        public Instant referenceTime() {
            return view.startsAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId().orElse(null);
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    /**
     * Một lượt vào tỷ lệ tham gia: Có mặt hoặc Vắng có lý do từ chối, sức khỏe, khác. 014 chỉ trả số lượt theo buổi nên mỗi
     * lượt mang khóa buổi và số thứ tự.
     */
    public record AttendanceRecord(SessionStatisticsView session, String result, int index) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.ACTIVITY_SESSION:" + session.sessionId() + ":" + result + ":" + index;
        }

        @Override
        public Instant referenceTime() {
            return session.startsAt();
        }

        @Override
        public Long floorAreaId() {
            return session.floorAreaId().orElse(null);
        }

        @Override
        public Long residentId() {
            return null;
        }

        boolean present() {
            return PRESENT.equals(result);
        }
    }

    /** Một chuyến đi; mốc xếp khoảng là giờ về dự kiến ban đầu. */
    public record TripRecord(TripStatisticsView view, String overdue) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.TRIP:" + view.tripId();
        }

        @Override
        public Instant referenceTime() {
            return view.plannedReturnAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId().orElse(null);
        }

        @Override
        public Long residentId() {
            return null;
        }

        /** Giờ về dự kiến hiện hành: lần gia hạn cuối, không có thì giờ về dự kiến. */
        Instant expectedReturn() {
            return view.extendedReturnAt().isEmpty()
                    ? view.plannedReturnAt()
                    : view.extendedReturnAt().getLast();
        }
    }

    private final ObjectProvider<ActivityStatisticsApi> statistics;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public ActivityRecords(
            ObjectProvider<ActivityStatisticsApi> statistics, ParameterApi parameters, BusinessTime time) {
        this.statistics = statistics;
        this.parameters = parameters;
        this.time = time;
    }

    public Stream<SessionRecord> sessions(ScopeFilter rough, Instant from, Instant to) {
        ActivityStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.sessions(rough, first(from), last(to)).map(SessionRecord::new);
    }

    public Stream<AttendanceRecord> attendance(ScopeFilter rough, Instant from, Instant to) {
        return sessions(rough, from, to).flatMap(s -> {
            List<AttendanceRecord> rows = new ArrayList<>();
            for (int i = 0; i < s.view().present(); i++) {
                rows.add(new AttendanceRecord(s.view(), PRESENT, i));
            }
            s.view().absentByReason().forEach((reason, n) -> {
                if (COUNTED_ABSENCES.contains(reason)) {
                    for (int i = 0; i < n; i++) {
                        rows.add(new AttendanceRecord(s.view(), reason, i));
                    }
                }
            });
            return rows.stream();
        });
    }

    /** Điểm danh buổi mang nhãn "ghi nhận muộn" (FR-032), mỗi lượt một bản ghi. */
    public Stream<AttendanceRecord> lateAttendance(ScopeFilter rough, Instant from, Instant to) {
        return sessions(rough, from, to)
                .flatMap(s -> java.util.stream.IntStream.range(0, s.view().lateRecorded())
                        .mapToObj(i -> new AttendanceRecord(s.view(), LATE_RECORDED, i)));
    }

    /**
     * Chuyến kèm dấu quá giờ về (BR-M04-17, Q-172): {@code OVERDUE} khi điểm danh về sau giờ về dự kiến hiện hành +
     * CFG-M04-07 hoặc chưa điểm danh về khi đã qua mốc đó; {@code OVERDUE_DOUBLE} khi tới mốc giờ về + 2 × CFG-M04-07;
     * rỗng là không quá giờ.
     */
    public Stream<TripRecord> trips(ScopeFilter rough, Instant from, Instant to) {
        ActivityStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Duration grace = parameters.read(ParameterCode.CFG_M04_07).asDuration();
        Instant now = time.now();
        return api.trips(rough, first(from), last(to)).map(v -> {
            TripRecord plain = new TripRecord(v, null);
            Instant end = v.actualReturnAt().orElse(now);
            Instant expected = plain.expectedReturn();
            String overdue = !end.isBefore(expected.plus(grace.multipliedBy(2)))
                    ? OVERDUE_DOUBLE
                    : end.isAfter(expected.plus(grace)) ? OVERDUE : null;
            return new TripRecord(v, overdue);
        });
    }

    private static LocalDate first(Instant from) {
        return LocalDate.ofInstant(from, BusinessTime.ZONE);
    }

    private static LocalDate last(Instant to) {
        return LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
    }

    static DetailRow sessionRow(SessionRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityId", r.view().activityId());
        result.put("trip", r.view().trip());
        result.put("registered", r.view().registered());
        result.put("present", r.view().present());
        result.put("absentByReason", r.view().absentByReason());
        result.put("notRecorded", r.view().notRecorded());
        return row(r.sourceKey(), "ACTIVITY_SESSION", r.view().status(), r, result);
    }

    static DetailRow attendanceRow(AttendanceRecord r) {
        return row(
                r.sourceKey(),
                "ATTENDANCE",
                r.result(),
                r,
                Map.of("activityId", r.session().activityId()));
    }

    static DetailRow tripRow(TripRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expectedReturnAt", r.expectedReturn());
        result.put("actualReturnAt", r.view().actualReturnAt().orElse(null));
        result.put("missingOnReturn", r.view().missingOnReturn());
        return row(r.sourceKey(), "TRIP", r.overdue(), r, result);
    }

    private static DetailRow row(String key, String type, String status, MetricRecord r, Map<String, Object> result) {
        return new DetailRow(
                key,
                "014",
                type,
                status,
                r.referenceTime(),
                r.floorAreaId(),
                null,
                null,
                null,
                null,
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
