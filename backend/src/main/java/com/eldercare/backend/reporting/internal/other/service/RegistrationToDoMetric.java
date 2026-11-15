package com.eldercare.backend.reporting.internal.other.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code REGISTRATION.TO_DO} ({@code KB.CAN_LAM}; FR-020): khai báo tạm trú cần làm lúc xem (018): cần lập, quá hạn, sắp hết
 * hạn, cần xóa đăng ký; bốn ô của nguồn, chỉ người xem toàn viện (Quản lý viện, Hành chính).
 */
@Component
@Transactional(readOnly = true)
public class RegistrationToDoMetric implements MetricSource<RegistrationToDoMetric.ToDoRecord> {

    public static final String CODE = "REGISTRATION.TO_DO";

    public record ToDoRecord(String category, int count) implements MetricRecord {
        @Override
        public String sourceKey() {
            return "M02.REGISTRATION_TO_DO:" + category;
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }

        BigDecimal value() {
            return BigDecimal.valueOf(count);
        }
    }

    private static final Metric<ToDoRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.RESIDENCE_REGISTRATION,
            "Khai báo tạm trú cần làm",
            "Cần lập, quá hạn, sắp hết hạn, cần xóa đăng ký lúc xem",
            "018 ResidenceRegistrationStatisticsApi.summary",
            new Metric.Computation.Aggregate<>(Metric.Computation.Aggregate.Kind.SUM, ToDoRecord::value),
            ToDoRecord::category,
            r -> DetailRow.simple(
                    r.sourceKey(), "018", "REGISTRATION", r.category(), null, null, null, Map.of("count", r.count())));

    private final ObjectProvider<ResidenceRegistrationStatisticsApi> statistics;

    public RegistrationToDoMetric(ObjectProvider<ResidenceRegistrationStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    @Override
    public Metric<ToDoRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<ToDoRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        ResidenceRegistrationStatisticsApi api = statistics.getIfAvailable();
        if (api == null || !rough.facilityWide()) return Stream.empty();
        var s = api.summary();
        return Stream.of(
                new ToDoRecord("TO_CREATE", s.toCreate()),
                new ToDoRecord("OVERDUE", s.overdue()),
                new ToDoRecord("EXPIRED", s.expired()),
                new ToDoRecord("TO_DEREGISTER", s.toDeregister()));
    }
}
