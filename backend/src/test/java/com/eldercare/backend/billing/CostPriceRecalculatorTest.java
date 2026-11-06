package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.admission.api.events.PriceVersionCreated;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.CostRepricingStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.billing.internal.pricing.service.*;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

/** T013: các điều kiện chọn khoản/chống lặp độc lập với controller còn mở ở Phase 3. */
class CostPriceRecalculatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);
    private final CostRepricingStore store = mock(CostRepricingStore.class);
    private final CostLock lock = mock(CostLock.class);
    private final ExistingCostPricing pricing = mock(ExistingCostPricing.class);
    private final ContractReadApi contracts = mock(ContractReadApi.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final StatusHistoryApi history = mock(StatusHistoryApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CostPriceRecalculator service =
            new CostPriceRecalculator(store, lock, pricing, contracts, markers, history, audit, time, events);

    private RepriceableCost row(Set<String> flags) {
        return new RepriceableCost(
                91,
                11,
                81,
                71,
                21,
                "CARE_TASK_RESULT",
                DAY,
                "DRAFT",
                "OPEN",
                0,
                new CostRecalculation.Calculation(new BigDecimal("0.125"), null, null, null),
                flags,
                Map.of("serviceId", 21L),
                Map.of());
    }

    @BeforeEach
    void source() {
        when(store.missingPrice(21, DAY, null)).thenReturn(List.of(new RepriceCandidate(91, 11, 81)));
        when(time.now()).thenReturn(Instant.parse("2026-11-06T02:00:00Z"));
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), anyMap()))
                .thenReturn(true);
    }

    @Test
    void FR_012_newPriceFillsOnlyTheMissingPriceAndPreservesActualQuantity() {
        var row = row(Set.of("MISSING_UNIT_PRICE", "CREATED_IN_ADVANCE"));
        var next = new CostPricingSnapshot(
                new CostRecalculation.Calculation(
                        new BigDecimal("0.125"), new BigDecimal("256"), null, new BigDecimal("32")),
                Set.of(),
                Map.of("serviceId", 21L, "priceVersionId", 31L),
                Map.of());
        when(store.lock(91, 11)).thenReturn(Optional.of(row));
        when(pricing.calculate(eq(row), anyMap())).thenReturn(Optional.of(next));

        service.priceCreated(new PriceVersionCreated(21, DAY, null));

        var ordered = inOrder(lock, store);
        ordered.verify(store).missingPrice(21, DAY, null);
        ordered.verify(lock).statement(11, 81);
        ordered.verify(store).lock(91, 11);
        ordered.verify(store).apply(row, next);
        verify(history).add(eq(CostRecalculation.TRACK), eq(91L), any());
        verify(audit).write(any());
        var published = ArgumentCaptor.forClass(CostItemChanged.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getValue().notificationRequests()).hasSize(1);
        assertThat(next.mergeFlags(row.flags())).containsExactly("CREATED_IN_ADVANCE");
    }

    @Test
    void FR_012_aDuplicateDeliveryDoesNotReadSourcesAgainOrAppendMoreHistory() {
        when(store.lock(91, 11)).thenReturn(Optional.of(row(Set.of("MISSING_UNIT_PRICE"))));
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), anyMap()))
                .thenReturn(false);

        service.priceCreated(new PriceVersionCreated(21, DAY, null));

        verifyNoInteractions(pricing, history, audit, events);
        verify(store, never()).apply(any(), any());
    }

    @Test
    void BR_M11_02_anAlreadyPricedItemIsNotRepricedByANewCatalogueVersion() {
        when(store.lock(91, 11)).thenReturn(Optional.of(row(Set.of())));

        service.priceCreated(new PriceVersionCreated(21, DAY, null));

        verifyNoInteractions(pricing, history, audit, events, markers);
    }

    @Test
    void DBR_17_anItemFinalizedAfterTheCandidateScanIsNotTouched() {
        when(store.lock(91, 11)).thenReturn(Optional.empty());

        service.priceCreated(new PriceVersionCreated(21, DAY, null));

        verifyNoInteractions(pricing, history, audit, events, markers);
    }

    @Test
    void FR_020_unchangedNumbersWithDifferentDecimalScalesDoNotCreateFakeChanges() {
        var row = row(Set.of("MISSING_UNIT_PRICE"));
        var next = new CostPricingSnapshot(
                new CostRecalculation.Calculation(new BigDecimal("0.1250"), null, null, null),
                Set.of("MISSING_UNIT_PRICE"),
                Map.of("serviceId", 21L),
                Map.of());
        when(store.lock(91, 11)).thenReturn(Optional.of(row));
        when(pricing.calculate(eq(row), anyMap())).thenReturn(Optional.of(next));
        when(store.sameJson(anyMap(), anyMap())).thenReturn(true);

        service.priceCreated(new PriceVersionCreated(21, DAY, null));

        verify(store, never()).apply(any(), any());
        verifyNoInteractions(history, audit, events);
    }

    @Test
    void FR_020_addendumRepricesTheCheckedItemAndPublishesTheNeedToCheckAgain() {
        var checked = new RepriceableCost(
                91,
                11,
                81,
                71,
                21,
                "CARE_TASK_RESULT",
                DAY,
                "CHECKED",
                "AWAITING_FINALIZATION",
                3,
                new CostRecalculation.Calculation(BigDecimal.ONE, new BigDecimal("100"), null, new BigDecimal("100")),
                Set.of(),
                Map.of(),
                Map.of());
        var next = new CostPricingSnapshot(
                new CostRecalculation.Calculation(BigDecimal.ONE, new BigDecimal("200"), null, new BigDecimal("200")),
                Set.of(),
                Map.of("serviceId", 21L, "contractId", 31L),
                Map.of());
        when(store.addendum(11, DAY)).thenReturn(List.of(new RepriceCandidate(91, 11, 81)));
        when(store.lock(91, 11)).thenReturn(Optional.of(checked));
        when(contracts.contentAtDate(11, DAY))
                .thenReturn(Optional.of(new ContractContentView(
                        31,
                        2,
                        DAY,
                        41L,
                        JsonMapper.builder().build().readTree("{\"residenceType\":\"LONG_TERM_RESIDENTIAL\"}"),
                        Map.of())));
        when(pricing.calculate(eq(checked), anyMap())).thenReturn(Optional.of(next));

        service.addendumApplied(new AddendumApplied(41, 31, 11, Set.of("SERVICES"), DAY));

        verify(store).apply(checked, next);
        verify(history)
                .add(
                        eq(CostRecalculation.TRACK),
                        eq(91L),
                        argThat((HistoryEntry<CostRecalculation> entry) -> entry != null
                                && entry.valueBefore().equals("CHECKED")
                                && entry.value().equals("DRAFT")
                                && entry.bySystem()
                                && entry.details().resetToDraft()));
        verify(events)
                .publishEvent(argThat((Object value) -> value instanceof CostItemChanged changed
                        && changed.notificationRequests().size() == 1));
    }
}
