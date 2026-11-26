package com.eldercare.backend.care.internal.careplan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Q-448 (FR-058): một mốc phát sinh còn Đang mở khi chưa có phiên bản được duyệt lập sau mốc đó và chưa có lần "Không cần
 * thay đổi" từ mốc đó trở đi. Phiên bản bị trả lại, bỏ trước khi duyệt hoặc lập trước mốc không đóng yêu cầu.
 */
class CarePlanReviewRuleTest {
    private static final Instant RAISED = Instant.parse("2030-03-09T07:20:00Z");
    private static final CarePlanReviewRaised FALL = new CarePlanReviewRaised("M05.INCIDENT", 9L, "9", RAISED);

    private static CarePlan plan(CarePlanStatus status, Instant createdAt, Instant approvedAt) {
        return new CarePlan(
                1L,
                11L,
                2,
                status,
                LocalDate.of(2030, 3, 10),
                null,
                null,
                null,
                3L,
                createdAt,
                null,
                approvedAt,
                false,
                null,
                null,
                null,
                null,
                0);
    }

    @Test
    void Q448_noPlanNoDismissal_open() {
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(), List.of())).containsExactly(FALL);
    }

    @Test
    void Q448_approvedPlanCreatedAfterRaise_closes() {
        var approved = plan(CarePlanStatus.PENDING_EFFECT, RAISED.plusSeconds(60), RAISED.plusSeconds(600));
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(approved), List.of()))
                .isEmpty();
    }

    @Test
    void Q448_supersededButApprovedPlanStillCloses() {
        var superseded = plan(CarePlanStatus.CANCELLED, RAISED.plusSeconds(60), RAISED.plusSeconds(600));
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(superseded), List.of()))
                .isEmpty();
    }

    @Test
    void Q448_approvingDraftCreatedBeforeRaise_keepsOpen() {
        var older = plan(CarePlanStatus.EFFECTIVE, RAISED.minusSeconds(3600), RAISED.plusSeconds(600));
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(older), List.of()))
                .containsExactly(FALL);
    }

    @Test
    void Q448_draftReturnedOrCancelledBeforeApproval_keepsOpen() {
        var draft = plan(CarePlanStatus.DRAFT, RAISED.plusSeconds(60), null);
        var cancelled = plan(CarePlanStatus.CANCELLED, RAISED.plusSeconds(120), null);
        var pending = plan(CarePlanStatus.PENDING_APPROVAL, RAISED.plusSeconds(180), null);
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(draft, cancelled, pending), List.of()))
                .containsExactly(FALL);
    }

    @Test
    void Q448_dismissalAtOrAfterRaise_closes_newerRaiseStaysOpen() {
        var later = new CarePlanReviewRaised("M01.ASSESSMENT", 5L, "5", RAISED.plusSeconds(3600));
        assertThat(CarePlanReviewRule.open(List.of(FALL, later), List.of(), List.of(RAISED)))
                .containsExactly(later);
        assertThat(CarePlanReviewRule.open(List.of(FALL), List.of(), List.of(RAISED.minusSeconds(1))))
                .containsExactly(FALL);
    }
}
