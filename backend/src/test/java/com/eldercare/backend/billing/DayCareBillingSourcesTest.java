package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.Attendance;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.billing.internal.source.service.DayCareBillingSources;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * 010 T011 (FR-005b, FR-009): buổi bán trú có lịch có mã nguồn theo người, ngày, thứ tự buổi; giá tháng chia số buổi có
 * lịch của cả tháng, bỏ ngày khu nghỉ; ngày khu nghỉ và hợp đồng không có dịch vụ buổi bán trú không lập được.
 */
class DayCareBillingSourcesTest {
    private static final long RESIDENT = 42;
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 13);
    private static final LocalDate CLOSED = LocalDate.of(2026, 10, 20);

    private final ResidencyDayApi days = mock(ResidencyDayApi.class);
    private final ContractReadApi contracts = mock(ContractReadApi.class);
    private final DayCareCalendarApi calendar = mock(DayCareCalendarApi.class);
    private final UnitPriceApi prices = mock(UnitPriceApi.class);
    private final BillingSourceIndexStore index = mock(BillingSourceIndexStore.class);
    private final DayCareBillingSources sources = new DayCareBillingSources(days, contracts, calendar, prices, index);

    @BeforeEach
    void schedule() {
        // Lịch đến thứ Ba; tháng 10/2026 có 4 thứ Ba (6, 13, 20, 27), ngày 20 khu nghỉ.
        when(contracts.attendanceOn(eq(RESIDENT), any())).thenAnswer(call -> {
            LocalDate d = call.getArgument(1);
            return d.getDayOfWeek() == DayOfWeek.TUESDAY
                    ? List.of(new AttendanceScheduleView(d, LocalTime.of(8, 0), LocalTime.of(16, 0)))
                    : List.of();
        });
        when(calendar.onDate(eq(RESIDENT), any()))
                .thenAnswer(call ->
                        new DayCareCalendarView(call.getArgument(1), CLOSED.equals(call.getArgument(1)), List.of()));
        when(prices.service(7))
                .thenReturn(Optional.of(new ServiceView(
                        7, ServiceType.DAY_CARE_SESSION, "Buổi bán trú", "buổi", BillingPeriod.MONTH, true, null)));
        when(index.find(anyLong(), eq("DAY_CARE_SESSION"), any(), any())).thenReturn(List.of());
        when(index.effectiveItems(anyLong(), eq("DAY_CARE_SESSION"), anyList())).thenReturn(Map.of());
    }

    private void contract(String services) {
        when(contracts.contentAtDate(eq(RESIDENT), any()))
                .thenReturn(Optional.of(new ContractContentView(
                        31,
                        1,
                        LocalDate.of(2026, 1, 1),
                        null,
                        JsonMapper.builder()
                                .build()
                                .readTree("{\"residencyFeeBillingPeriod\":\"MONTH\",\"services\":" + services + "}"),
                        Map.of())));
    }

    @Test
    void FR_009_monthlyPrice_isSplitOverScheduledSessionsOfTheWholeMonth_withoutClosureDays() {
        contract("[{\"serviceId\":7}]");
        when(days.dayCareSessions(RESIDENT, TUESDAY, TUESDAY))
                .thenReturn(List.of(new DayCareSessionView(TUESDAY, 1, Attendance.PRESENT, 100, false)));

        var candidate = sources.between(RESIDENT, TUESDAY, TUESDAY).getFirst();

        assertThat(candidate.sourceId()).isEqualTo("42:2026-10-13:1");
        assertThat(candidate.serviceId()).isEqualTo(7L);
        assertThat(candidate.blockedCode()).isNull();
        assertThat(candidate.facts())
                .containsEntry("billingPeriod", "MONTH")
                .containsEntry("scheduledSessionCount", 3)
                .containsEntry("lastScheduledSession", false);
    }

    @Test
    void FR_009_lastScheduledSessionOfTheMonth_isMarked() {
        contract("[{\"serviceId\":7}]");
        var last = LocalDate.of(2026, 10, 27);
        when(days.dayCareSessions(RESIDENT, last, last))
                .thenReturn(List.of(new DayCareSessionView(last, 1, Attendance.ABSENT_NOTIFIED, 50, false)));

        var candidate = sources.between(RESIDENT, last, last).getFirst();

        assertThat(candidate.facts()).containsEntry("lastScheduledSession", true);
        assertThat(candidate.factor()).isEqualByComparingTo("50");
    }

    @Test
    void FR_005b_closureDay_isNotBillable() {
        contract("[{\"serviceId\":7}]");
        when(days.dayCareSessions(RESIDENT, CLOSED, CLOSED))
                .thenReturn(List.of(new DayCareSessionView(CLOSED, 1, null, null, true)));

        assertThat(sources.between(RESIDENT, CLOSED, CLOSED).getFirst().blockedCode())
                .isEqualTo("NOT_A_BILLABLE_EVENT");
    }

    @Test
    void FR_008_contractWithoutDayCareService_isNotCreated() {
        contract("[]");
        when(days.dayCareSessions(RESIDENT, TUESDAY, TUESDAY))
                .thenReturn(List.of(new DayCareSessionView(TUESDAY, 1, Attendance.PRESENT, 100, false)));

        var candidate = sources.between(RESIDENT, TUESDAY, TUESDAY).getFirst();

        assertThat(candidate.blockedCode()).isEqualTo("CONDITION_NOT_MET");
        assertThat(candidate.blockedReason()).contains("dịch vụ buổi bán trú");
    }
}
