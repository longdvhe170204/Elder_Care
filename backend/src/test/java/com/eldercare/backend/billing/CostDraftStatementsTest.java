package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.internal.period.domain.DraftCostStatement;
import com.eldercare.backend.billing.internal.period.persistence.DraftCostStatementStore;
import com.eldercare.backend.billing.internal.period.service.CostDraftStatements;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** T011: bước nhận Nháp độc lập metadata nguồn; chưa phải test lệnh HTTP/nhật ký đầy đủ. */
class CostDraftStatementsTest {
    private final CostLock lock = mock(CostLock.class);
    private final DraftCostStatementStore store = mock(DraftCostStatementStore.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CostDraftStatements service = new CostDraftStatements(lock, store, time);

    private DraftCostStatement row(String status, int version) {
        return new DraftCostStatement(91, 81, 42, status, false, null, version);
    }

    @Test
    void FR_002_firstDraftLocksTheResidentBeforeCreatingItsOnlyRegularStatement() {
        var at = Instant.parse("2026-11-06T02:00:00Z");
        when(store.periodStatusForShare(81)).thenReturn("OPEN");
        when(store.regularForUpdate(42, 81)).thenReturn(Optional.empty());
        when(time.now()).thenReturn(at);
        when(store.createRegular(42, 81, 52, at)).thenReturn(row("OPEN", 0));

        var target = service.receiveNewDraft(42, 81, 52);

        assertThat(target.created()).isTrue();
        assertThat(target.statement().id()).isEqualTo(91);
        var order = inOrder(lock, store);
        order.verify(lock).resident(42);
        order.verify(store).periodStatusForShare(81);
        order.verify(store).regularForUpdate(42, 81);
        order.verify(store).createRegular(42, 81, 52, at);
    }

    @Test
    void FR_004_openStatementIsReusedWithoutCreatingAnotherOne() {
        when(store.periodStatusForShare(81)).thenReturn("OPEN");
        when(store.regularForUpdate(42, 81)).thenReturn(Optional.of(row("OPEN", 3)));

        var target = service.receiveNewDraft(42, 81, 52);

        assertThat(target.created()).isFalse();
        assertThat(target.reopened()).isFalse();
        assertThat(target.statement().version()).isEqualTo(3);
        verify(store).periodStatusForShare(81);
        verify(store).regularForUpdate(42, 81);
        verifyNoMoreInteractions(store);
        verifyNoInteractions(time);
    }

    @Test
    void FR_004_awaitingStatementIsReopenedAndItsCurrentVersionIsReturned() {
        when(store.periodStatusForShare(81)).thenReturn("AWAITING_FINALIZATION");
        when(store.regularForUpdate(42, 81))
                .thenReturn(Optional.of(row("AWAITING_FINALIZATION", 3)), Optional.of(row("OPEN", 4)));

        var target = service.receiveNewDraft(42, 81, 52);

        assertThat(target.reopened()).isTrue();
        assertThat(target.statement().status()).isEqualTo("OPEN");
        assertThat(target.statement().version()).isEqualTo(4);
        verify(store).reopen(42, 91);
        verifyNoInteractions(time);
    }

    @Test
    void FR_003_finalizedStatementRequestsTheAdjustmentPathWithoutBeingModified() {
        when(store.periodStatusForShare(81)).thenReturn("OPEN");
        when(store.regularForUpdate(42, 81)).thenReturn(Optional.of(row("FINALIZED", 3)));

        var target = service.receiveNewDraft(42, 81, 52);

        assertThat(target.requiresAdjustment()).isTrue();
        assertThat(target.created()).isFalse();
        assertThat(target.reopened()).isFalse();
        verify(store).periodStatusForShare(81);
        verify(store).regularForUpdate(42, 81);
        verifyNoMoreInteractions(store);
        verifyNoInteractions(time);
    }

    @Test
    void FR_003_finalizedPeriodDoesNotReceiveANewRegularStatementForALateSource() {
        when(store.periodStatusForShare(81)).thenReturn("FINALIZED");
        when(store.regularForUpdate(42, 81)).thenReturn(Optional.empty());

        var target = service.receiveNewDraft(42, 81, 52);

        assertThat(target.requiresAdjustment()).isTrue();
        assertThat(target.statement()).isNull();
        verify(store).periodStatusForShare(81);
        verify(store).regularForUpdate(42, 81);
        verifyNoMoreInteractions(store);
        verifyNoInteractions(time);
    }
}
