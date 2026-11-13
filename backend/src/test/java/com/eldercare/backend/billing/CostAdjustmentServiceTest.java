package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange;
import com.eldercare.backend.billing.internal.adjustment.service.CostAdjustmentService;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.item.domain.CostItemRecord;
import com.eldercare.backend.billing.internal.item.persistence.*;
import com.eldercare.backend.billing.internal.item.service.CostDestination;
import com.eldercare.backend.billing.internal.period.domain.CostStatementRecord;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CostAdjustmentServiceTest {
    private final CostItemStore items = mock(CostItemStore.class);
    private final CostCreationStore creation = mock(CostCreationStore.class);
    private final CostStatementStore statements = mock(CostStatementStore.class);
    private final CostDestination destination = mock(CostDestination.class);
    private final CostLock locks = mock(CostLock.class);
    private final CostAccess access = mock(CostAccess.class);
    private final RecordEventApi changes = mock(RecordEventApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final Reason reason = Reason.of("Miễn giảm theo quyết định");

    private CostAdjustmentService service(String origin, String status) {
        var row = new CostItemRecord(1, 2, 42, 3, CostType.SERVICE, origin, status, 300L, Set.of(), null, 8, null);
        when(items.item(1)).thenReturn(row);
        when(items.lockedItem(1)).thenReturn(row);
        when(creation.adjustmentTotal(1)).thenReturn(BigDecimal.ZERO);
        when(statements.lockedStatement(2))
                .thenReturn(new CostStatementRecord(
                        2, 3, 42, YearMonth.of(2026, 11), "REGULAR", false, null, "OPEN", null, 0));
        when(time.now()).thenReturn(Instant.parse("2026-11-07T03:00:00Z"));
        return new CostAdjustmentService(
                items,
                creation,
                statements,
                mock(CostRecalculationStore.class),
                destination,
                locks,
                access,
                changes,
                mock(AuditLogApi.class),
                time,
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void FR_026_lateAdditionMayReceiveAWaiverInItsOwnUnfinalizedStatement() {
        service("ADJUSTMENT", "DRAFT").adjust(1, -100, "WAIVER", reason, null);
        verify(creation)
                .adjustment(
                        eq(1L),
                        eq(2L),
                        eq(3L),
                        eq(-100L),
                        eq("WAIVER"),
                        eq(reason.value()),
                        isNull(),
                        anyLong(),
                        any());
        verifyNoInteractions(destination);
    }

    @Test
    void FR_026_includesPendingAdjustmentsBeforeAllowingAnotherReduction() {
        var service = service("EVENT_BASED", "DRAFT");
        when(creation.adjustmentTotal(1)).thenReturn(new BigDecimal("-300"));
        assertThatThrownBy(() -> service.adjust(1, -1, "WAIVER", reason, null)).isInstanceOf(BusinessException.class);
        verify(creation, never())
                .adjustment(
                        anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString(), any(), anyLong(), any());
        var ordered = inOrder(locks, statements, items, creation);
        ordered.verify(locks).resident(42);
        ordered.verify(statements).lockPeriod(3);
        ordered.verify(locks).statement(42, 2);
        ordered.verify(items).lockedItem(1);
        ordered.verify(creation).adjustmentTotal(1);
    }

    @Test
    void FR_027_rejectsAChangeBelongingToAnotherItem() {
        var service = service("EVENT_BASED", "FINALIZED");
        var now = time.now();
        when(changes.require(PostFinalizationChange.KIND, 9))
                .thenReturn(new RecordEvent<>(
                        9, new EntityRef("M11.COST_ITEM", 99), null, null, null, true, now, null, now, null));
        assertThatThrownBy(() -> service.adjust(1, -1, "SOURCE_CHANGE", reason, 9L))
                .isInstanceOf(InvalidValueException.class);
        verifyNoInteractions(destination);
        verify(creation, never()).adjustmentTotal(anyLong());
    }
}
