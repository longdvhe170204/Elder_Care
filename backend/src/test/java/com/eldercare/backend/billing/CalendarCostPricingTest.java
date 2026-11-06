package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.*;
import com.eldercare.backend.billing.internal.pricing.service.CalendarCostPricing;
import com.eldercare.backend.billing.internal.pricing.service.CalendarCostPricing.MonthSchedule;
import com.eldercare.backend.billing.internal.pricing.service.CalendarCostPricing.Slot;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService;
import com.eldercare.backend.common.error.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** T013/T014: giá theo đúng dịch vụ/hợp đồng; không giả định tổng phí phòng và chăm sóc. */
class CalendarCostPricingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 5);
    private final ContractReadApi contracts = mock(ContractReadApi.class);
    private final UnitPriceApi prices = mock(UnitPriceApi.class);
    private final DayCareCalendarApi calendar = mock(DayCareCalendarApi.class);
    private final CostPricingService eventPricing = new CostPricingService(contracts, prices);
    private final CalendarCostPricing service = new CalendarCostPricing(contracts, calendar, eventPricing);

    private ContractContentView content(String kind, String period, String price, boolean included) {
        var json = JsonMapper.builder()
                .build()
                .readTree("{\"residenceType\":\"" + kind
                        + "\",\"residencyFeeBillingPeriod\":\"" + period
                        + "\",\"services\":[{\"serviceId\":21,\"inPackage\":" + included
                        + "}],\"arrivalSchedule\":[]}");
        return new ContractContentView(
                31, 2, DAY.minusDays(1), 41L, json, price == null ? Map.of() : Map.of(21L, new BigDecimal(price)));
    }

    private ResidencyDayView residency(LocalDate day, boolean bedReleased) {
        return new ResidencyDayView(day, 31L, 51L, "ABSENT", 61L, bedReleased, true, true, false);
    }

    @Test
    void BR_M11_09_monthlyResidencyUsesCalendarDaysAndKeepsContractAndSourceSnapshots() {
        when(contracts.contentAtDate(11, DAY))
                .thenReturn(Optional.of(content("LONG_TERM_RESIDENTIAL", "MONTH", "9300000", false)));
        var result = service.residencyDay(11, 21, residency(DAY, false));
        assertThat(result.price().unitPrice()).isEqualByComparingTo("300000");
        assertThat(result.price().amount()).isEqualByComparingTo("300000");
        assertThat(result.sourceUnitPrice()).isEqualByComparingTo("9300000");
        assertThat(result.price().basis().contractId()).isEqualTo(31L);
        assertThat(result.price().basis().contentVersion()).isEqualTo(2);
        assertThat(result.sourceValues()).containsEntry("absenceId", 61L).containsEntry("notYetAdmitted", true);
        verifyNoInteractions(prices);
    }

    @Test
    void BR_M11_09_lastCalendarDayCarriesTheWholeMonthRoundingDifference() {
        var last = LocalDate.of(2026, 10, 31);
        when(contracts.contentAtDate(11, last))
                .thenReturn(Optional.of(content("LONG_TERM_RESIDENTIAL", "MONTH", "1000000", false)));
        var result = service.residencyDay(11, 21, residency(last, false));
        assertThat(result.price().amount()).isEqualByComparingTo("32260");
    }

    @Test
    void Q_395_releasedBedIsRejectedBeforeLookingUpAnyPrice() {
        assertThatThrownBy(() -> service.residencyDay(11, 21, residency(DAY, true)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(contracts, prices);
    }

    @Test
    void FR_012_missingSignedResidencyPriceDoesNotFallBackToTheCatalogueOrZero() {
        when(contracts.contentAtDate(11, DAY))
                .thenReturn(Optional.of(content("LONG_TERM_RESIDENTIAL", "DAY", null, false)));
        var result = service.residencyDay(11, 21, residency(DAY, false));
        assertThat(result.price().missingUnitPrice()).isTrue();
        assertThat(result.price().amount()).isNull();
        assertThat(result.price().basis().contractId()).isEqualTo(31L);
        verifyNoInteractions(prices);
    }

    @Test
    void FR_010_includedCalendarServiceHasZeroAmountEvenWithoutASeparateSignedPrice() {
        when(contracts.contentAtDate(11, DAY))
                .thenReturn(Optional.of(content("LONG_TERM_RESIDENTIAL", "DAY", null, true)));
        var result = service.residencyDay(11, 21, residency(DAY, false));
        assertThat(result.price().amount()).isEqualByComparingTo("0");
        assertThat(result.price().missingUnitPrice()).isFalse();
    }

    private MonthSchedule schedule() {
        return new MonthSchedule(
                11,
                YearMonth.from(DAY),
                List.of(new Slot(DAY, 1), new Slot(DAY.plusDays(1), 1), new Slot(DAY.plusDays(2), 1)));
    }

    @Test
    void Q_138_lastScheduledSessionGetsTheRemainderBeforeTheAttendanceFactor() {
        var last = DAY.plusDays(2);
        when(contracts.contentAtDate(11, last)).thenReturn(Optional.of(content("DAY_CARE", "MONTH", "1000000", false)));
        var result = service.dayCareScheduled(
                11, 21, new DayCareSessionView(last, 1, Attendance.ABSENT_NOTIFIED, 50, false), schedule());
        assertThat(result.price().unitPrice()).isEqualByComparingTo("333334");
        assertThat(result.price().amount()).isEqualByComparingTo("166667");
        assertThat(result.scheduledSessionCount()).isEqualTo(3);
        assertThat(result.lastScheduledSession()).isTrue();
    }

    @Test
    void FR_005b_aSessionWithoutAttendanceFactorIsNotTreatedAsFreeOrFullyPresent() {
        assertThatThrownBy(() ->
                        service.dayCareScheduled(11, 21, new DayCareSessionView(DAY, 1, null, null, false), schedule()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(prices);
    }

    @Test
    void FR_005b_aClosureSessionIsNotPriced() {
        assertThatThrownBy(() -> service.dayCareScheduled(
                        11, 21, new DayCareSessionView(DAY, 1, Attendance.PRESENT, 100, true), schedule()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void Q_140_adHocSessionUnderAMonthlyContractUsesTheSeparateSessionCataloguePrice() {
        when(contracts.contentAtDate(11, DAY)).thenReturn(Optional.of(content("DAY_CARE", "MONTH", "9300000", false)));
        when(prices.unitPriceAt(22, DAY, true))
                .thenReturn(Optional.of(new UnitPriceView(22, 71, new BigDecimal("250000"), DAY, null)));
        var result = service.dayCareAdHoc(11, 21, 22, DAY);
        assertThat(result.price().amount()).isEqualByComparingTo("250000");
        assertThat(result.price().basis().priceVersionId()).isEqualTo(71L);
        assertThat(result.factorPercent()).isEqualByComparingTo("100");
        verify(prices).unitPriceAt(22, DAY, true);
    }

    @Test
    void Q_140_adHocSessionUsesTheSignedSessionPriceWhenPresent() {
        when(contracts.contentAtDate(11, DAY)).thenReturn(Optional.of(content("DAY_CARE", "SESSION", "200000", false)));
        var result = service.dayCareAdHoc(11, 21, 22, DAY);
        assertThat(result.price().amount()).isEqualByComparingTo("200000");
        assertThat(result.price().basis().contractId()).isEqualTo(31L);
        verifyNoInteractions(prices);
    }

    @Test
    void FR_009_scheduleIsBoundToItsResidentAndCalendarMonth() {
        assertThatThrownBy(() -> service.dayCareScheduled(
                        12, 21, new DayCareSessionView(DAY, 1, Attendance.PRESENT, 100, false), schedule()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_009_monthScheduleUsesAllSignedDatesAndExcludesClosuresWithoutAddingAdHocSessions() {
        var json = JsonMapper.builder().build().readTree("""
                {"residenceType":"DAY_CARE","residencyFeeBillingPeriod":"MONTH","services":[],
                 "arrivalSchedule":[
                    {"date":"2026-10-05","arrivalTime":"14:00"},
                    {"date":"2026-10-05","arrivalTime":"08:00"},
                    {"date":"2026-10-06","arrivalTime":"08:00"},
                    {"date":"2026-10-07","arrivalTime":"08:00"}]}
                """);
        var content = new ContractContentView(31, 1, DAY, null, json, Map.of());
        for (int index = 0; index < 3; index++) {
            var day = DAY.plusDays(index);
            when(contracts.contentAtDate(11, day)).thenReturn(Optional.of(content));
            when(calendar.onDate(11, day)).thenReturn(new DayCareCalendarView(day, index == 1, List.of()));
        }

        var schedule = service.monthSchedule(11, YearMonth.from(DAY));

        assertThat(schedule.slots()).containsExactly(new Slot(DAY, 1), new Slot(DAY, 2), new Slot(DAY.plusDays(2), 1));
        verify(contracts).contentAtDate(11, LocalDate.of(2026, 10, 31));
    }

    @Test
    void FR_009_aZeroAttendanceFactorIsNotAFreePackageOrMissingPrice() {
        when(contracts.contentAtDate(11, DAY)).thenReturn(Optional.of(content("DAY_CARE", "MONTH", "900000", false)));
        var result = service.dayCareScheduled(
                11, 21, new DayCareSessionView(DAY, 1, Attendance.ABSENT_UNNOTIFIED, 0, false), schedule());
        assertThat(result.price().unitPrice()).isEqualByComparingTo("300000");
        assertThat(result.price().amount()).isEqualByComparingTo("0");
        assertThat(result.price().includedInPackage()).isFalse();
        assertThat(result.price().missingUnitPrice()).isFalse();
    }

    @Test
    void DBR_16_adHocSnapshotKeepsTheActualServiceBehindItsPriceVersion() {
        when(contracts.contentAtDate(11, DAY)).thenReturn(Optional.of(content("DAY_CARE", "MONTH", "9300000", false)));
        when(prices.unitPriceAt(22, DAY, true))
                .thenReturn(Optional.of(new UnitPriceView(22, 71, new BigDecimal("250000"), DAY, null)));
        var quote = service.dayCareAdHoc(11, 21, 22, DAY);
        var snapshot = com.eldercare.backend.billing.internal.pricing.service.CostPricingSnapshots.calendar(quote);
        assertThat(snapshot.unitPriceBasis()).containsEntry("serviceId", 22L).containsEntry("priceVersionId", 71L);
        assertThat(snapshot.sourceValues())
                .containsEntry("contractedServiceId", 21L)
                .containsEntry("adHocServiceId", 22L);
    }

    @Test
    void DBR_16_monthlyAddendumUsesTheSignedVersionForEachSourceDay() {
        var before = LocalDate.of(2026, 11, 19);
        var after = before.plusDays(1);
        var base = content("LONG_TERM_RESIDENTIAL", "MONTH", "9000000", false);
        when(contracts.contentAtDate(11, before))
                .thenReturn(Optional.of(new ContractContentView(
                        31,
                        1,
                        before.withDayOfMonth(1),
                        null,
                        base.content(),
                        Map.of(21L, new BigDecimal("9000000")))));
        when(contracts.contentAtDate(11, after))
                .thenReturn(Optional.of(new ContractContentView(
                        31, 2, after, 41L, base.content(), Map.of(21L, new BigDecimal("12000000")))));

        var first = service.residencyDay(11, 21, residency(before, false));
        var second = service.residencyDay(
                11, 21, new ResidencyDayView(after, 31L, 52L, "PRESENT", null, false, false, false, false));

        assertThat(first.price().amount()).isEqualByComparingTo("300000");
        assertThat(first.price().basis().contentVersion()).isEqualTo(1);
        assertThat(second.price().amount()).isEqualByComparingTo("400000");
        assertThat(second.price().basis().addendumId()).isEqualTo(41L);
        verifyNoInteractions(prices);
    }
}
