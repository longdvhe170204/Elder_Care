package com.eldercare.backend.reporting.internal.quality.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.ActivityStatisticsApi;
import com.eldercare.backend.care.api.dto.QualityCheckStatisticsView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mục kiểm tra chất lượng cho {@code QC.*} (FR-037) đọc {@code ActivityStatisticsApi.qualityChecks} của 014: kết quả
 * ({@code PASS}, {@code FAIL}, {@code NOT_CHECKABLE}, {@code OVERDUE}, {@code PENDING}) và vai trò người kiểm tra do 014
 * ghi. Mốc xếp khoảng là lúc ghi kết quả; mục chưa có kết quả không có mốc.
 */
@Component
@Transactional(readOnly = true)
public class QualityCheckRecords {

    static final String PASS = "PASS";
    static final String FAIL = "FAIL";
    static final Set<String> CHECKED = Set.of(PASS, FAIL, "NOT_CHECKABLE");

    /** Một mục kiểm tra của một công việc gốc. */
    public record QualityCheckRecord(QualityCheckStatisticsView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.QUALITY_CHECK_ITEM:" + view.itemId();
        }

        @Override
        public Instant referenceTime() {
            return view.checkedAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return null;
        }

        boolean passOrFail() {
            return PASS.equals(view.result()) || FAIL.equals(view.result());
        }

        String performerKey() {
            return view.performerStaffId() == null ? "UNKNOWN" : Long.toString(view.performerStaffId());
        }
    }

    private final ObjectProvider<ActivityStatisticsApi> statistics;

    public QualityCheckRecords(ObjectProvider<ActivityStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<QualityCheckRecord> in(ScopeFilter rough, Instant from, Instant to) {
        ActivityStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate first = LocalDate.ofInstant(from, BusinessTime.ZONE);
        LocalDate last = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        return api.qualityChecks(rough, first, last).map(QualityCheckRecord::new);
    }

    static DetailRow row(QualityCheckRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("careTaskId", r.view().careTaskId());
        result.put("shiftId", r.view().shiftId());
        result.put("checkerRole", r.view().checkerRole());
        return new DetailRow(
                r.sourceKey(),
                "014",
                "QUALITY_CHECK_ITEM",
                r.view().result(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                null,
                null,
                r.view().performerStaffId() == null
                        ? null
                        : r.view().performerStaffId().toString(),
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
