package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.billing.internal.period.domain.CostPeriodRecord;
import com.eldercare.backend.billing.internal.period.persistence.CostPeriodStore;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CostDailyTaskTest {
    @Test
    void FR_045_calendarCreationIncludesABasisForTheSchedulerAudit() {
        var store = mock(CostPeriodStore.class);
        var coverage = mock(CostBillingCoverage.class);
        var parameters = mock(ParameterApi.class);
        var markers = mock(ReminderMarkers.class);
        var notifications = mock(CostNotifications.class);
        var audit = mock(AuditLogApi.class);
        when(store.ensurePeriod(any(), any())).thenReturn(true);
        when(store.periodId(any())).thenReturn(42L);
        var task = new CostDailyTask(
                store,
                coverage,
                parameters,
                markers,
                notifications,
                mock(CostStatementStore.class),
                mock(CostItemStore.class),
                mock(CostLock.class),
                audit);
        task.run(Instant.parse("2026-11-01T00:00:00Z"));
        var entry = org.mockito.ArgumentCaptor.forClass(
                com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry.class);
        verify(audit).write(entry.capture());
        assertThat(entry.getValue().basis()).isNotNull();
        assertThat(entry.getValue().basis().ruleCode()).isEqualTo("010.FR-001");
    }

    @Test
    void FR_031_dailyTaskRemindsMissingStatementsWithoutCreatingBusinessRecords() {
        var store = mock(CostPeriodStore.class);
        var coverage = mock(CostBillingCoverage.class);
        var parameters = mock(ParameterApi.class);
        var markers = mock(ReminderMarkers.class);
        var notifications = mock(CostNotifications.class);
        var month = YearMonth.of(2026, 10);
        when(store.periods())
                .thenReturn(
                        List.of(new CostPeriodRecord(10, month, "AWAITING_FINALIZATION", LocalDate.of(2026, 11, 30))));
        when(coverage.expected(month)).thenReturn(Set.of(123L));
        when(store.hasRegularStatement(10, 123)).thenReturn(false);
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        var task = new CostDailyTask(
                store,
                coverage,
                parameters,
                markers,
                notifications,
                mock(CostStatementStore.class),
                mock(CostItemStore.class),
                mock(CostLock.class),
                mock(AuditLogApi.class));
        int changed = task.run(Instant.parse("2026-11-02T00:00:00Z"));
        assertThat(changed).isEqualTo(1);
        verify(notifications)
                .staff(
                        eq("COST_STATEMENT_MISSING"),
                        eq("M11.COST_PERIOD"),
                        eq(10L),
                        eq(123L),
                        anyString(),
                        anyString(),
                        eq(com.eldercare.backend.common.Role.OFFICE_STAFF),
                        any(),
                        isNull());
        verify(store).ensurePeriod(eq(YearMonth.of(2026, 11)), any());
        verify(store).advanceItems(eq(LocalDate.of(2026, 11, 2)));
        verify(store, never()).closePeriod(anyLong(), any(), any());
    }

    @Test
    void FR_044_dueStatementWithUnpricedItems_remindsOfficeStaffAndManager() {
        var store = mock(CostPeriodStore.class);
        var coverage = mock(CostBillingCoverage.class);
        var markers = mock(ReminderMarkers.class);
        var notifications = mock(CostNotifications.class);
        var statements = mock(CostStatementStore.class);
        var items = mock(CostItemStore.class);
        var month = YearMonth.of(2026, 10);
        when(store.periods())
                .thenReturn(
                        List.of(new CostPeriodRecord(10, month, "AWAITING_FINALIZATION", LocalDate.of(2026, 11, 30))));
        when(coverage.expected(month)).thenReturn(Set.of());
        var open = mock(com.eldercare.backend.billing.internal.period.domain.CostStatementRecord.class);
        when(open.id()).thenReturn(5L);
        when(open.residentId()).thenReturn(123L);
        when(open.status()).thenReturn("OPEN");
        when(statements.list(10L, null, null)).thenReturn(List.of(open));
        var unpriced = mock(com.eldercare.backend.billing.internal.item.domain.CostItemRecord.class);
        when(unpriced.flags()).thenReturn(Set.of("MISSING_UNIT_PRICE"));
        when(items.ofStatement(5L)).thenReturn(List.of(unpriced));
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        var task = new CostDailyTask(
                store,
                coverage,
                mock(ParameterApi.class),
                markers,
                notifications,
                statements,
                items,
                mock(CostLock.class),
                mock(AuditLogApi.class));
        task.run(Instant.parse("2026-12-01T00:00:00Z"));
        for (var role : List.of(
                com.eldercare.backend.common.Role.OFFICE_STAFF, com.eldercare.backend.common.Role.NURSING_HOME_MANAGER))
            verify(notifications)
                    .staff(
                            eq("COST_FINALIZATION_DUE"),
                            eq(5L),
                            eq(123L),
                            anyString(),
                            anyString(),
                            eq(role),
                            any(),
                            isNull());
    }

    @Test
    void FR_001_finalizationDeadlineComesFromTheCurrentConfiguration() {
        var store = mock(CostPeriodStore.class);
        var coverage = mock(CostBillingCoverage.class);
        var parameters = mock(ParameterApi.class);
        var markers = mock(ReminderMarkers.class);
        var notifications = mock(CostNotifications.class);
        var month = YearMonth.of(2026, 10);
        when(store.periods()).thenReturn(List.of(new CostPeriodRecord(10, month, "OPEN", null)));
        when(coverage.expected(month)).thenReturn(Set.of());
        when(parameters.read(ParameterCode.CFG_M11_01))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M11_01,
                        ParameterKind.TABLE,
                        JsonMapper.builder().build().readTree("{\"monthsAfterPeriodEnd\":2,\"dayOfMonth\":15}")));
        var task = new CostDailyTask(
                store,
                coverage,
                parameters,
                markers,
                notifications,
                mock(CostStatementStore.class),
                mock(CostItemStore.class),
                mock(CostLock.class),
                mock(AuditLogApi.class));
        task.run(Instant.parse("2026-11-01T00:00:00Z"));
        verify(store).closePeriod(eq(10L), eq(LocalDate.of(2026, 12, 15)), any());
    }
}
