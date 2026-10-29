package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.staffing.service.UnconfirmedHandoversMetric;
import com.eldercare.backend.reporting.internal.staffing.service.UnconfirmedHandoversMetric.HandoverRecord;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.HandoverReportView;
import com.eldercare.backend.support.fake.staffing.FakeStaffStatistics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * Chỉ tiêu {@code SHIFT.UNCONFIRMED_HANDOVERS} (tên logic {@code CA.BAN_GIAO_CHUA_XAC_NHAN}; 016 T023, FR-020): bàn giao Đã
 * lập hoặc Có ý kiến, cộng Bản nháp của ca đang Chờ bàn giao, theo {@code StaffStatisticsApi.openHandovers}; mỗi mục kèm
 * trạng thái và thời gian đã chờ từ giờ kết thúc ca trước. Chỉ có ở dashboard, theo phạm vi hiện hành.
 */
class MetricShiftHandoversTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    private final FakeStaffStatistics statistics = new FakeStaffStatistics();

    private final UnconfirmedHandoversMetric source = new UnconfirmedHandoversMetric(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(StaffStatisticsApi.class),
            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));

    private static HandoverReportView handover(long id, long floor, String status, Instant shiftEndsAt) {
        return new HandoverReportView(id, id * 10, floor, status, null, null, false, shiftEndsAt);
    }

    @Test
    void FR_020_countsOpenHandovers_withStatusAndWaitingTime() {
        statistics.handovers(List.of(
                handover(1, 101, "PREPARED", NOW.minus(Duration.ofMinutes(120))),
                handover(2, 101, "HAS_COMMENTS", NOW.minus(Duration.ofMinutes(30))),
                handover(3, 102, "DRAFT", NOW.minus(Duration.ofMinutes(5))),
                handover(4, 101, "CONFIRMED", NOW.minus(Duration.ofMinutes(300)))));
        Metric<HandoverRecord> metric = source.metric();

        List<HandoverRecord> included =
                metric.included(source.current(ScopeFilter.FACILITY, null, null), ReportScope.FACILITY::covers);
        MetricValue value = metric.compute(included);
        List<DetailRow> rows = metric.details(included, null);

        assertThat(metric.code()).isEqualTo("SHIFT.UNCONFIRMED_HANDOVERS");
        assertThat(metric.group()).isEqualTo(MetricGroup.HANDOVERS);
        assertThat(value.value()).isEqualByComparingTo("3");
        assertThat(value.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(Tuple.tuple("DRAFT", 1), Tuple.tuple("HAS_COMMENTS", 1), Tuple.tuple("PREPARED", 1));
        assertThat(rows).extracting(DetailRow::status).containsExactly("PREPARED", "HAS_COMMENTS", "DRAFT");
        assertThat(rows).extracting(r -> r.result().get("waitingMinutes")).containsExactly(120L, 30L, 5L);
        assertThat(rows).extracting(DetailRow::residentId).containsOnlyNulls();
    }
}
