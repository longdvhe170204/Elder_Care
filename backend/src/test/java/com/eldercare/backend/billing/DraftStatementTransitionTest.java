package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.period.domain.DraftStatementTransition;
import org.junit.jupiter.api.Test;

/** T011: quyết định nhận Nháp của FR-002/003/004, chưa phải thao tác ghi hoặc điều chỉnh. */
class DraftStatementTransitionTest {
    @Test
    void FR_002_theFirstAcceptedCostNeedsAnOpenRegularStatement() {
        var result = DraftStatementTransition.forNewDraft(null);
        assertThat(result.createStatement()).isTrue();
        assertThat(result.reopenStatement()).isFalse();
        assertThat(result.requiresAdjustment()).isFalse();
    }

    @Test
    void FR_004_anOpenStatementIsReusedWithoutAStateTransition() {
        var result = DraftStatementTransition.forNewDraft("OPEN");
        assertThat(result.createStatement()).isFalse();
        assertThat(result.reopenStatement()).isFalse();
        assertThat(result.requiresAdjustment()).isFalse();
    }

    @Test
    void FR_004_aNewDraftReopensTheAwaitingStatement() {
        var result = DraftStatementTransition.forNewDraft("AWAITING_FINALIZATION");
        assertThat(result.reopenStatement()).isTrue();
        assertThat(result.createStatement()).isFalse();
        assertThat(result.requiresAdjustment()).isFalse();
    }

    @Test
    void FR_003_aFinalizedStatementIsNotReopenedOrReplacedWithADraft() {
        var result = DraftStatementTransition.forNewDraft("FINALIZED");
        assertThat(result.requiresAdjustment()).isTrue();
        assertThat(result.reopenStatement()).isFalse();
        assertThat(result.createStatement()).isFalse();
    }
}
