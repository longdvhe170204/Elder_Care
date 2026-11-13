package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.*;
import com.eldercare.backend.billing.internal.item.service.CostRecalculationService;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.billing.internal.pricing.service.*;
import com.eldercare.backend.billing.internal.source.service.CostSourceLookup;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

class CostRecalculationServiceTest {
    private final CostRecalculationStore store = mock(CostRecalculationStore.class);
    private final CostItemStore items = mock(CostItemStore.class);
    private final CostStatementStore statements = mock(CostStatementStore.class);
    private final CostPricingService pricing = mock(CostPricingService.class);
    private final StatusHistoryApi history = mock(StatusHistoryApi.class);
    private final RecordEventApi changes = mock(RecordEventApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CostSourceLookup lookup = mock(CostSourceLookup.class);
    private final Instant now = Instant.parse("2026-11-07T03:00:00Z");

    private CostRecalculationService service() {
        when(time.now()).thenReturn(now);
        return new CostRecalculationService(
                store,
                items,
                statements,
                mock(CostLock.class),
                pricing,
                mock(SourceCostPricing.class),
                lookup,
                history,
                changes,
                mock(CostNotifications.class),
                mock(AuditLogApi.class),
                time,
                mock(ApplicationEventPublisher.class),
                JsonMapper.builder().build());
    }

    private StoredCostCalculation row(String status) {
        var row = new StoredCostCalculation(
                1,
                42,
                2,
                3,
                status,
                "CARE_TASK_RESULT",
                "8",
                "UNIT",
                9L,
                LocalDate.of(2026, 11, 1),
                now,
                new CostRecalculation.Calculation(
                        new BigDecimal("2"), new BigDecimal("100"), null, new BigDecimal("200")),
                Map.of("quantity", 2));
        when(store.source("CARE_TASK_RESULT", "8", "UNIT")).thenReturn(List.of(row));
        when(store.lock(1)).thenReturn(row);
        return row;
    }

    @Test
    void BR_59_checkedItemRecalculatesAndReopensStatement() {
        var row = row("CHECKED");
        when(items.item(1))
                .thenReturn(new CostItemRecord(
                        1, 2, 42, 3, CostType.CONSUMABLE, "EVENT_BASED", "CHECKED", 200L, Set.of(), 7L, 6L, null));
        when(pricing.event(42, 9, row.date(), BigDecimal.ONE))
                .thenReturn(
                        new CostPricingService.PriceQuote(new BigDecimal("100"), new BigDecimal("100"), false, null));
        service().sourceChanged("CARE_TASK_RESULT", "8", "UNIT", BigDecimal.ONE, false, "CORRECTION:5", "event-5");
        var cost = ArgumentCaptor.forClass(com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost.class);
        verify(store).recalculate(eq(1L), cost.capture());
        assertThat(cost.getValue().amount()).isEqualByComparingTo("100");
        verify(history).add(eq(CostRecalculation.TRACK), eq(1L), any());
        verify(statements).touch(2, true);
        verifyNoInteractions(changes);
    }

    @Test
    void FR_027_finalizedItemOnlyRecordsSourceFactsAndNeverReprices() {
        row("FINALIZED");
        when(changes.list(eq(PostFinalizationChange.KIND), any())).thenReturn(List.of());
        when(changes.findByUniqueKey(eq(PostFinalizationChange.KIND), anyString()))
                .thenReturn(Optional.empty());
        when(changes.add(eq(PostFinalizationChange.KIND), any(), any())).thenAnswer(invocation -> {
            NewRecordEvent<PostFinalizationChange> value = invocation.getArgument(2);
            assertThat(value.content().after()).doesNotContainKeys("amount", "difference", "suggestedAmount");
            return new RecordEvent<>(
                    10, new EntityRef("M11.COST_ITEM", 1), value.content(), null, null, true, now, null, now, null);
        });
        service().sourceChanged("CARE_TASK_RESULT", "8", "UNIT", null, true, "VOID:5", "event-5");
        verify(changes).add(eq(PostFinalizationChange.KIND), eq(new EntityRef("M11.COST_ITEM", 1)), any());
        verify(store, never()).recalculate(anyLong(), any());
        verify(store, never()).cancel(anyLong(), anyString(), any());
        verify(statements, never()).touch(anyLong(), anyBoolean());
        verifyNoInteractions(pricing, history);
    }

    @Test
    void FR_012_priceEventDoesNotRepriceAnItemAlreadyResolvedWhileWaitingForTheLock() {
        var earlier = row("DRAFT");
        when(store.affected(42L, 9L, earlier.date(), earlier.date(), true)).thenReturn(List.of(earlier));
        when(items.item(1))
                .thenReturn(new CostItemRecord(
                        1, 2, 42, 3, CostType.CONSUMABLE, "EVENT_BASED", "DRAFT", 200L, Set.of(), null, 6L, null));
        service().refresh(42L, 9L, earlier.date(), earlier.date(), true, "PRICE_VERSION_CREATED");
        verifyNoInteractions(pricing, history, changes);
        verify(store, never()).recalculate(anyLong(), any());
    }
}
