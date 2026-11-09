package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.finalperiod.service.FinalBillingWindow;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.persistence.CostCreationStore;
import com.eldercare.backend.billing.internal.item.service.CostDestination;
import com.eldercare.backend.billing.internal.period.domain.CostStatementRecord;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import com.eldercare.backend.billing.internal.pricing.service.SourceCostPricing;
import com.eldercare.backend.billing.internal.source.domain.*;
import com.eldercare.backend.billing.internal.source.service.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FinalPeriodSourceTest {
    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true"})
    void FR_034a_onlyDaysOutsideFinalizedRangeAffectTermination(boolean outside, boolean monthly) {
        var date = monthly ? LocalDate.of(2026, 12, 1) : LocalDate.of(2026, 11, 20);
        var lookup = mock(CostSourceLookup.class);
        var destination = mock(CostDestination.class);
        var creation = mock(CostCreationStore.class);
        var pricing = mock(SourceCostPricing.class);
        var time = mock(BusinessTime.class);
        when(time.today()).thenReturn(date.plusDays(2));
        when(time.now())
                .thenReturn(date.plusDays(2).atStartOfDay(BusinessTime.ZONE).toInstant());
        var finalStatement = new CostStatementRecord(
                1,
                2,
                3,
                YearMonth.from(monthly ? date.minusDays(1) : date),
                "REGULAR",
                true,
                monthly ? null : outside ? date.minusDays(1) : date,
                "FINALIZED",
                100L,
                1);
        var target =
                new CostStatementRecord(4, 5, 3, YearMonth.from(date), "SUPPLEMENTARY", false, null, "OPEN", null, 0);
        when(destination.finalizedFinalPeriod(3)).thenReturn(Optional.of(finalStatement));
        when(destination.adjustment(eq(3L), anyLong())).thenReturn(target);
        var source = new CostSourceCandidate(
                3,
                "RESIDENCY_DAY",
                "3:" + date,
                "DAY",
                date,
                date.atStartOfDay(BusinessTime.ZONE).toInstant(),
                6L,
                CostType.RESIDENCY_FEE,
                "ngày",
                "Phí lưu trú",
                BigDecimal.ONE,
                new BigDecimal("100"),
                Set.of(),
                null,
                null,
                null,
                Map.of());
        when(lookup.find(3, "RESIDENCY_DAY", date, date)).thenReturn(List.of(source));
        when(pricing.calculate(source))
                .thenReturn(new CalculatedCost(
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        new BigDecimal("100"),
                        BigDecimal.TEN,
                        Map.of(),
                        Map.of(),
                        Set.of()));
        var commands = new SourceCostCommands(
                lookup,
                mock(BillingSourceSelection.class),
                mock(CostAccess.class),
                mock(CostLock.class),
                destination,
                creation,
                mock(CostStatementStore.class),
                pricing,
                time,
                mock(AuditLogApi.class),
                mock(org.springframework.context.ApplicationEventPublisher.class),
                mock(CostNotifications.class),
                mock(FinalBillingWindow.class));
        commands.create(new SourceCostSelection(3, "RESIDENCY_DAY", null, null, date, date));
        var cost = org.mockito.ArgumentCaptor.forClass(CalculatedCost.class);
        verify(creation).source(eq(source), cost.capture(), eq(4L), eq(5L), eq(2L), anyLong(), any());
        assertThat(cost.getValue().flags()).contains("AFTER_FINALIZED_PERIOD");
        assertThat(cost.getValue().flags().contains("AFFECTS_TERMINATION")).isEqualTo(outside);
        verify(destination, never()).regular(anyLong(), any(), anyLong());
    }
}
