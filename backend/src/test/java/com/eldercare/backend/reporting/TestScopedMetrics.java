package com.eldercare.backend.reporting;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Chỉ tiêu giả cho test phạm vi và giới hạn trường (016 T011), một chỉ tiêu mỗi nhóm, thay chỉ tiêu thật tới khi 005, 007,
 * 010 có {@code *ThongKeApi} (docs/team-plan.md mục 8). Chỉ kiểm khung: nhóm, phạm vi, nhóm thông tin của kết quả.
 */
@TestConfiguration
class TestScopedMetrics {

    /** Tên thuốc trong kết quả, chỉ người xem được nhóm thông tin của chỉ tiêu mới thấy (FR-014, Q-133). */
    static final String RESTRICTED = "Paracetamol 500mg";

    static final String ITEM_CODE = "VT-001";

    record Event(String sourceKey, Instant referenceTime, Long floorAreaId, Long residentId) implements MetricRecord {}

    static final class EventSource implements MetricSource<Event> {

        final List<Event> events = new CopyOnWriteArrayList<>();

        private final Metric<Event> metric;

        EventSource(String code, MetricGroup group, InfoGroup resultGroup) {
            metric = new Metric<>(
                    code,
                    group,
                    "Chỉ tiêu test " + code,
                    "Số bản ghi trong khoảng",
                    "test",
                    new Metric.Computation.Count<>(e -> true),
                    e -> "ALL",
                    e -> new DetailRow(
                            e.sourceKey(),
                            "test",
                            "EVENT",
                            "OPEN",
                            e.referenceTime(),
                            e.floorAreaId(),
                            "Tầng " + e.floorAreaId(),
                            e.residentId(),
                            "Nguyễn Văn A",
                            "Điều dưỡng A",
                            Map.of("item", RESTRICTED, "itemCode", ITEM_CODE),
                            resultGroup,
                            Map.of("itemCode", ITEM_CODE),
                            false,
                            false,
                            true));
        }

        @Override
        public Metric<Event> metric() {
            return metric;
        }

        @Override
        public Stream<Event> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
            return events.stream()
                    .filter(e -> rough.facilityWide()
                            || rough.floorAreaIds().contains(e.floorAreaId())
                            || (e.residentId() != null && rough.residentIds().contains(e.residentId())))
                    .filter(e -> !e.referenceTime().isBefore(from)
                            && e.referenceTime().isBefore(to));
        }
    }

    static final String RESIDENTS = "TEST.RESIDENTS";
    static final String TASKS = "TEST.TASKS";
    static final String HEALTH = "TEST.HEALTH";
    static final String NON_HEALTH = "TEST.NON_HEALTH_INCIDENTS";
    static final String COSTS = "TEST.COSTS";
    static final String STAFFING = "TEST.STAFFING";

    @Bean
    EventSource testResidents() {
        return new EventSource(RESIDENTS, MetricGroup.RESIDENTS, null);
    }

    @Bean
    EventSource testTasks() {
        return new EventSource(TASKS, MetricGroup.CARE_TASKS, null);
    }

    @Bean
    EventSource testHealth() {
        return new EventSource(HEALTH, MetricGroup.HEALTH_EVENTS, InfoGroup.HEALTH);
    }

    @Bean
    EventSource testNonHealthIncidents() {
        return new EventSource(NON_HEALTH, MetricGroup.NON_HEALTH_INCIDENTS, InfoGroup.HEALTH);
    }

    @Bean
    EventSource testCosts() {
        return new EventSource(COSTS, MetricGroup.INCURRED_COSTS, InfoGroup.MEDICATION);
    }

    @Bean
    EventSource testStaffing() {
        return new EventSource(STAFFING, MetricGroup.STAFFING_SHIFTS, null);
    }
}
