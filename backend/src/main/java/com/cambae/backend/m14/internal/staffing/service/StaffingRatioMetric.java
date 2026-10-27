package com.cambae.backend.m14.internal.staffing.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.StaffStatisticsApi;
import com.cambae.backend.m09.api.dto.StaffingRatioView;
import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code SHIFT.STAFFING_RATIO} ({@code CA.TY_LE_PHUC_VU}): giá trị và tình trạng do 008 tính, 016 không tính lại
 * (FR-002, BR-M09-02, Q-79). Dashboard (FR-020, US1 #4) là ca đang diễn ra, theo lần tính mới nhất; báo cáo (FR-070,
 * Q-200) là ca đã kết thúc có giờ bắt đầu trong khoảng, theo lần tính cuối trước khi ca kết thúc, "không đạt" theo dấu
 * "từng không đạt". Ngưỡng CFG-M09-01 chưa cấu hình thì {@code NOT_CONFIGURED}, không hiện đạt hay không đạt (Edge
 * Cases). Mỗi dòng chi tiết là một ca: tầng/khu, mẫu ca, tỷ lệ, ngưỡng, số nhân viên được tính, tổng trọng số.
 */
@Component
@Transactional(readOnly = true)
public class StaffingRatioMetric implements MetricSource<StaffingRatioMetric.StaffingRatioRecord> {

    public static final String CODE = "SHIFT.STAFFING_RATIO";

    /** Tình trạng của ca trong chỉ tiêu; khóa chia nhóm. */
    public enum Status {
        MET,
        NOT_MET,
        NOT_CONFIGURED
    }

    /** Lần tính tỷ lệ đưa vào chỉ tiêu của một ca; mốc xếp khoảng là giờ bắt đầu ca. */
    public record StaffingRatioRecord(StaffingRatioView view, Status status) implements MetricRecord {

        public long shiftId() {
            return view.shiftId();
        }

        @Override
        public String sourceKey() {
            return "M09.WORK_SHIFT:" + view.shiftId();
        }

        @Override
        public Instant referenceTime() {
            return view.shiftStartsAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<StaffingRatioRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.STAFFING_RATIO,
            "Tỷ lệ phục vụ",
            "Dashboard: tỷ lệ phục vụ và tình trạng đạt theo khu của ca đang diễn ra. Báo cáo: mỗi ca đã diễn ra có giờ bắt"
                    + " đầu trong khoảng, theo lần tính cuối trước khi ca kết thúc; ca không đạt là ca có lần tính nào"
                    + " trong thời gian ca không đạt ngưỡng CFG-M09-01",
            "008 StaffStatisticsApi.staffingRatios",
            new Metric.Computation.Count<>(r -> true),
            r -> r.status().name(),
            StaffingRatioMetric::row);

    private final ObjectProvider<StaffStatisticsApi> statistics;
    private final BusinessTime time;

    public StaffingRatioMetric(ObjectProvider<StaffStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<StaffingRatioRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<StaffingRatioRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        StaffStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        LocalDate first = LocalDate.ofInstant(from, BusinessTime.ZONE);
        LocalDate last = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        return api.staffingRatios(rough, first, last)
                .filter(v ->
                        !v.shiftStartsAt().isBefore(from) && v.shiftStartsAt().isBefore(to))
                .filter(v -> !v.shiftEndsAt().isAfter(now))
                .filter(v -> filters.shiftId() == null || filters.shiftId() == v.shiftId())
                .map(v -> new StaffingRatioRecord(v, status(v, v.everBelowThreshold())));
    }

    /** Ca đang diễn ra lúc xem; ca qua đêm bắt đầu từ hôm qua. */
    @Override
    public Stream<StaffingRatioRecord> current(ScopeFilter rough, Instant from, Instant to) {
        StaffStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        LocalDate today = time.today();
        return api.staffingRatios(rough, today.minusDays(1), today)
                .filter(v -> !v.shiftStartsAt().isAfter(now) && v.shiftEndsAt().isAfter(now))
                .map(v -> new StaffingRatioRecord(v, status(v, !v.passed())));
    }

    private static Status status(StaffingRatioView v, boolean notMet) {
        if (v.threshold() == null) {
            return Status.NOT_CONFIGURED;
        }
        return notMet ? Status.NOT_MET : Status.MET;
    }

    private static DetailRow row(StaffingRatioRecord r) {
        StaffingRatioView v = r.view();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ratio", v.ratio());
        result.put("threshold", v.threshold());
        result.put("staffCount", v.staffCount());
        result.put("totalWeight", v.totalWeight());
        result.put("passed", v.passed());
        result.put("everBelowThreshold", v.everBelowThreshold());
        result.put("shiftTemplateId", v.shiftTemplateId());
        result.put("shiftEndsAt", v.shiftEndsAt());
        result.put("calculatedAt", v.calculatedAt());
        return new DetailRow(
                r.sourceKey(),
                "008",
                "SHIFT",
                r.status().name(),
                v.shiftStartsAt(),
                v.floorAreaId(),
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
