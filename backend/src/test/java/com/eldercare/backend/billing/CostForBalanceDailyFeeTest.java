package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import com.eldercare.backend.billing.internal.provision.service.CostForBalance;
import com.eldercare.backend.billing.internal.provision.service.CostSummaries;
import com.eldercare.backend.billing.internal.provision.service.FinalizedCostViews;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * 017 FR-016, Q-223 (dòng chờ E ở team-plan 08/11): "phí lưu trú một ngày" cho bốn trường hợp: hợp đồng tháng (phí ngày cơ
 * sở), hợp đồng ngày (đơn giá ngày), bán trú giá tháng (giá ÷ số ngày của tháng), bán trú giá buổi (giá × số buổi có lịch 7
 * ngày tới ÷ 7); làm tròn tới đồng; chưa có giá thì rỗng.
 */
class CostForBalanceDailyFeeTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 10); // thứ Ba; tháng 11 có 30 ngày
    private final ContractReadApi contracts = mock(ContractReadApi.class);
    private final UnitPriceApi prices = mock(UnitPriceApi.class);
    private final CostForBalance port = new CostForBalance(
            mock(CostSummaries.class),
            mock(CostStatementStore.class),
            mock(FinalizedCostViews.class),
            mock(CostProvisionStore.class),
            contracts,
            prices);

    private void contract(String json, long service, String price) {
        when(contracts.contentAtDate(42, DAY))
                .thenReturn(Optional.of(new ContractContentView(
                        1,
                        1,
                        DAY.minusMonths(1),
                        null,
                        JsonMapper.builder().build().readTree(json),
                        Map.of(service, new BigDecimal(price)))));
    }

    private void residencyService() {
        when(prices.residencyFeeServiceOf(3))
                .thenReturn(Optional.of(new ServiceView(
                        9, ServiceType.RESIDENCY_FEE, "Phí lưu trú", "tháng", BillingPeriod.MONTH, true, null)));
    }

    private void dayCareService() {
        when(prices.service(7))
                .thenReturn(Optional.of(new ServiceView(
                        7, ServiceType.DAY_CARE_SESSION, "Bán trú", "buổi", BillingPeriod.SESSION, true, null)));
    }

    @Test
    void FR_016_monthlyContract_usesBaseDailyFee() {
        residencyService();
        contract("{\"residencyFeeBillingPeriod\":\"MONTH\",\"roomTypeId\":3}", 9, "9000000");
        assertThat(port.baseDailyFee(42, DAY)).contains(300_000L);
    }

    @Test
    void FR_016_dailyContract_usesDailyPrice() {
        residencyService();
        contract("{\"residencyFeeBillingPeriod\":\"DAY\",\"roomTypeId\":3}", 9, "320000");
        assertThat(port.baseDailyFee(42, DAY)).contains(320_000L);
    }

    @Test
    void Q_223_dayCareMonthlyPrice_isDividedByDaysOfTheMonth() {
        dayCareService();
        contract(
                "{\"residenceType\":\"DAY_CARE\",\"residencyFeeBillingPeriod\":\"MONTH\",\"services\":[{\"serviceId\":7}]}",
                7,
                "3000001");
        assertThat(port.baseDailyFee(42, DAY)).contains(100_000L);
    }

    @Test
    void Q_223_dayCareSessionPrice_timesScheduledSessionsInNextSevenDays_dividedBySeven() {
        dayCareService();
        contract(
                "{\"residenceType\":\"DAY_CARE\",\"residencyFeeBillingPeriod\":\"SESSION\",\"services\":[{\"serviceId\":7}]}",
                7,
                "200000");
        // Thứ Hai, Tư, Sáu: 3 buổi trong 7 ngày từ thứ Ba 10/11.
        when(contracts.attendanceOn(anyLong(), any())).thenAnswer(call -> {
            LocalDate d = call.getArgument(1);
            return switch (d.getDayOfWeek()) {
                case MONDAY, WEDNESDAY, FRIDAY ->
                    List.of(new AttendanceScheduleView(d, LocalTime.of(8, 0), LocalTime.of(16, 0)));
                default -> List.of();
            };
        });
        assertThat(port.baseDailyFee(42, DAY)).contains(85_714L);
    }

    @Test
    void FR_016_noPrice_isEmpty() {
        dayCareService();
        when(contracts.contentAtDate(eq(42L), any())).thenReturn(Optional.empty());
        assertThat(port.baseDailyFee(42, DAY)).isEmpty();
        assertThat(DayOfWeek.from(DAY)).isEqualTo(DayOfWeek.TUESDAY);
    }
}
