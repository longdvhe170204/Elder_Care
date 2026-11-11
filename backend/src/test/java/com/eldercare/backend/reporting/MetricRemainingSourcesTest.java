package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.admission.api.dto.RegistrationSummaryView;
import com.eldercare.backend.billing.api.BalanceStatisticsApi;
import com.eldercare.backend.billing.api.CostStatisticsApi;
import com.eldercare.backend.billing.api.ProcurementStatisticsApi;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatusView;
import com.eldercare.backend.billing.api.dto.CostItemReportView;
import com.eldercare.backend.billing.api.dto.CostPeriodView;
import com.eldercare.backend.billing.api.dto.CostStatementReportView;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters.CostMode;
import com.eldercare.backend.reporting.internal.other.service.BalanceLowOrOwingMetric;
import com.eldercare.backend.reporting.internal.other.service.ContainmentZonesMetric;
import com.eldercare.backend.reporting.internal.other.service.CostEventGeneratedRateMetric;
import com.eldercare.backend.reporting.internal.other.service.CostItems;
import com.eldercare.backend.reporting.internal.other.service.CostPostFinalizationAdjustmentsMetric;
import com.eldercare.backend.reporting.internal.other.service.CostPurchasingMetric;
import com.eldercare.backend.reporting.internal.other.service.CostStatementsMetric;
import com.eldercare.backend.reporting.internal.other.service.CostSummaryMetric;
import com.eldercare.backend.reporting.internal.other.service.EmptyAwaitingTransferMetric;
import com.eldercare.backend.reporting.internal.other.service.RegistrationToDoMetric;
import com.eldercare.backend.reporting.internal.other.service.StaffRequestsMetric;
import com.eldercare.backend.roombedallocation.api.BedStatisticsApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.staffing.api.SwapStatisticsApi;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 016 T008, T014, T020, T024 (chỉ tiêu chờ nguồn tới 2026-11-10, nay nguồn đã có thật): giường trống chờ chuyển người, khu
 * đang khoanh vùng, yêu cầu đổi ca và nghỉ đột xuất, giờ làm theo tháng, năm chỉ tiêu chi phí, số dư thấp hoặc còn nợ,
 * khai báo tạm trú cần làm. Tính đúng trên view của nguồn (FR-002), bỏ khoản Đã hủy, phân chế độ đã chốt/tạm tính (FR-051).
 */
class MetricRemainingSourcesTest {

    private static final Instant FROM = Instant.parse("2026-10-31T17:00:00Z");
    private static final Instant TO = Instant.parse("2026-11-30T17:00:00Z");
    private static final BusinessTime TIME = time();

    private static BusinessTime time() {
        var t = mock(BusinessTime.class);
        when(t.now()).thenReturn(Instant.parse("2026-11-10T03:00:00Z"));
        when(t.today()).thenReturn(LocalDate.of(2026, 11, 10));
        return t;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(bean);
        return p;
    }

    private static <R extends MetricRecord> MetricValue value(MetricSource<R> source, ReportFilters filters) {
        var metric = source.metric();
        var included = metric.included(source.records(ScopeFilter.FACILITY, FROM, TO, filters), r -> true);
        return metric.compute(included);
    }

    private static Map<String, BigDecimal> breakdown(MetricValue v) {
        var result = new java.util.TreeMap<String, BigDecimal>();
        v.breakdown().forEach(b -> result.put(b.key(), b.value()));
        return result;
    }

    @Test
    void FR_020_emptyBedsAwaitingTransferAndContainmentZones() {
        var beds = mock(BedStatisticsApi.class);
        when(beds.emptyAwaitingTransferBedIds(any(), any())).thenReturn(Set.of(7L, 8L));
        var structure = mock(LocationStructureApi.class);
        when(structure.bed(anyLong())).thenReturn(Optional.empty());
        when(structure.bed(7L))
                .thenReturn(Optional.of(new LocationView(1L, "Khu A", 5L, "Tầng 2", 9L, "P203", 7L, "A")));
        assertThat(value(new EmptyAwaitingTransferMetric(provider(beds), structure, TIME), ReportFilters.NONE)
                        .value())
                .isEqualByComparingTo("2");

        var incidents = mock(IncidentStatisticsApi.class);
        when(incidents.containmentZones(any()))
                .thenReturn(List.of(new IsolationZoneView(3, Set.of(5L, 6L), Set.of(), FROM, null)));
        assertThat(value(new ContainmentZonesMetric(provider(incidents)), ReportFilters.NONE)
                        .value())
                .as("một khu trên hai tầng đếm một lần")
                .isEqualByComparingTo("1");
    }

    @Test
    void FR_072_FR_073_requestsByFinalStatusAndWorkingHours() {
        var swaps = mock(SwapStatisticsApi.class);
        when(swaps.requests(any(), any(), any()))
                .thenReturn(Stream.of(
                        new StaffRequestView(1, "SWAP", "APPROVED", 5L, FROM, FROM.plusSeconds(3600)),
                        new StaffRequestView(2, "LEAVE", "REJECTED", 5L, FROM, FROM.plusSeconds(7200)),
                        new StaffRequestView(3, "SWAP", "APPROVED", 5L, FROM, FROM.plusSeconds(60))));
        var requests = value(new StaffRequestsMetric(provider(swaps)), ReportFilters.NONE);
        assertThat(requests.value()).isEqualByComparingTo("3");
        assertThat(breakdown(requests))
                .containsEntry("LEAVE:REJECTED", BigDecimal.ONE)
                .containsEntry("SWAP:APPROVED", BigDecimal.valueOf(2));
    }

    @Test
    void FR_050_FR_053_costSummaryRateAndAdjustments_byMode() {
        var costs = mock(CostStatisticsApi.class);
        LocalDate d = LocalDate.of(2026, 11, 3);
        var items = List.of(
                item(1, CostType.RESIDENCY_FEE, "EVENT_BASED", 1_000_000L, Set.of(), "FINALIZED", null),
                item(2, CostType.MEDICATION, "MANUAL", 200_000L, Set.of(), "FINALIZED", null),
                item(3, CostType.MEDICATION, "EVENT_BASED", null, Set.of("MISSING_UNIT_PRICE"), "FINALIZED", null),
                item(4, CostType.RESIDENCY_FEE, "ADJUSTMENT", -100_000L, Set.of(), "APPROVED", "2026-10"),
                item(5, CostType.RESIDENCY_FEE, "MANUAL", 300_000L, Set.of(), "CANCELLED", null));
        when(costs.items(any(), any(), any())).thenAnswer(i -> items.stream());
        var api = provider(costs);
        var finalized = new ReportFilters(Set.of(), null, null, null, null, null, CostMode.FINALIZED);
        var provisional = new ReportFilters(Set.of(), null, null, null, null, null, CostMode.PROVISIONAL);

        assertThat(value(new CostSummaryMetric(new CostItems(api)), finalized).value())
                .as("đã chốt: khoản thiếu đơn giá không cộng")
                .isEqualByComparingTo("1200000");
        assertThat(value(new CostSummaryMetric(new CostItems(api)), provisional).value())
                .as("tạm tính: thêm khoản chưa hủy của bảng chưa chốt")
                .isEqualByComparingTo("1100000");
        var rate = value(new CostEventGeneratedRateMetric(new CostItems(api)), finalized);
        assertThat(rate.numerator()).isEqualByComparingTo("2");
        assertThat(rate.denominator()).isEqualByComparingTo("3");
        var adjustments = value(new CostPostFinalizationAdjustmentsMetric(new CostItems(api)), provisional);
        assertThat(adjustments.value()).isEqualByComparingTo("1");
        assertThat(breakdown(adjustments)).containsKey("2026-10");
    }

    @Test
    void FR_051_FR_055a_statementsAndPurchasingSeparateFromCosts() {
        var costs = mock(CostStatisticsApi.class);
        when(costs.periods())
                .thenReturn(Stream.of(
                        new CostPeriodView(
                                1,
                                "2026-11",
                                LocalDate.of(2026, 11, 1),
                                LocalDate.of(2026, 11, 30),
                                "OPEN",
                                null,
                                null),
                        new CostPeriodView(
                                2,
                                "2026-09",
                                LocalDate.of(2026, 9, 1),
                                LocalDate.of(2026, 9, 30),
                                "FINALIZED",
                                null,
                                null)));
        when(costs.statements(Optional.of(1L)))
                .thenReturn(Stream.of(
                        new CostStatementReportView(11, 41, 1, CostStatementType.REGULAR, false, "DRAFT", null),
                        new CostStatementReportView(12, 42, 1, CostStatementType.REGULAR, false, "FINALIZED", null)));
        var statements = value(new CostStatementsMetric(provider(costs)), ReportFilters.NONE);
        assertThat(statements.value()).isEqualByComparingTo("2");
        assertThat(breakdown(statements)).containsKeys("DRAFT", "FINALIZED");

        var procurement = mock(ProcurementStatisticsApi.class);
        when(procurement.byDepartment(any(), any(), any()))
                .thenReturn(Stream.of(
                        new ProcurementRequestView(
                                1,
                                3,
                                ProcurementType.ASSET,
                                ProcurementStatus.RECONCILED,
                                900,
                                800L,
                                750L,
                                LocalDate.of(2026, 11, 2),
                                LocalDate.of(2026, 11, 5)),
                        new ProcurementRequestView(
                                2,
                                3,
                                ProcurementType.SERVICE,
                                ProcurementStatus.APPROVED,
                                500,
                                400L,
                                null,
                                LocalDate.of(2026, 11, 2),
                                null)));
        var purchasing = value(new CostPurchasingMetric(provider(procurement)), ReportFilters.NONE);
        assertThat(purchasing.value())
                .as("thực chi khi có, chưa mua thì số được duyệt")
                .isEqualByComparingTo("1150");
    }

    @Test
    void FR_020_lowOrOwingBalancesAndRegistrationToDo() {
        var balances = mock(BalanceStatisticsApi.class);
        when(balances.statuses(any()))
                .thenReturn(Stream.of(
                        new BalanceStatusView(41, BalanceStatus.RUNNING_LOW, 100_000, 3),
                        new BalanceStatusView(42, BalanceStatus.IN_DEBT, -50_000, null),
                        new BalanceStatusView(43, BalanceStatus.NORMAL, 9_000_000, 90)));
        var low = value(new BalanceLowOrOwingMetric(provider(balances)), ReportFilters.NONE);
        assertThat(low.value()).isEqualByComparingTo("2");

        var registrations = mock(ResidenceRegistrationStatisticsApi.class);
        when(registrations.summary()).thenReturn(new RegistrationSummaryView(2, 1, 0, 3));
        var todo = value(new RegistrationToDoMetric(provider(registrations)), ReportFilters.NONE);
        assertThat(todo.value()).isEqualByComparingTo("6");
        assertThat(breakdown(todo)).containsEntry("TO_DEREGISTER", BigDecimal.valueOf(3));
    }

    private static CostItemReportView item(
            long id, CostType type, String origin, Long amount, Set<String> flags, String status, String period) {
        return new CostItemReportView(
                id,
                41,
                type,
                origin,
                9,
                null,
                null,
                LocalDate.of(2026, 11, 3),
                BigDecimal.ONE,
                amount,
                amount,
                flags,
                status,
                period == null ? null : 900L,
                period);
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
