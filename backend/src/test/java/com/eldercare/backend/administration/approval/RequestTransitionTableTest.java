package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.APPLIED;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.APPLY_FAILED;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.APPROVED;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.CANCELLED;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.DRAFT;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.PENDING_APPROVAL;
import static com.eldercare.backend.administration.approval.api.dto.ApprovalStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.internal.domain.RequestTransitionTable;
import com.eldercare.backend.administration.approval.internal.domain.RequestTransitionTable.Command;
import com.eldercare.backend.common.error.BusinessException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Bảng chuyển trạng thái chung (data-model mục 3; BR-M15-08, FR-031, FR-033, FR-040; 000 T082): mọi cặp (trạng thái,
 * lệnh), gồm chuyển bị cấm; chuyển mở rộng chỉ theo khai báo của loại yêu cầu. Hủy bắt buộc lý do kiểm ở
 * {@code ApprovalLifecycleIT} (lệnh có {@code @BusinessCommand(requiresReason = true)}).
 */
class RequestTransitionTableTest {

    private static final List<String> STATUSES =
            List.of(DRAFT, PENDING_APPROVAL, APPROVED, APPLIED, APPLY_FAILED, REJECTED, CANCELLED);

    /** Đích của các chuyển được phép; cặp không có ở đây phải bị chặn. */
    private static final Map<Command, Map<String, String>> ALLOWED = new EnumMap<>(Map.of(
            Command.EDIT_CONTENT, Map.of(DRAFT, DRAFT),
            Command.SUBMIT, Map.of(DRAFT, PENDING_APPROVAL),
            Command.APPROVE, Map.of(PENDING_APPROVAL, APPROVED),
            Command.REJECT, Map.of(PENDING_APPROVAL, REJECTED),
            Command.CANCEL, Map.of(DRAFT, CANCELLED, PENDING_APPROVAL, CANCELLED, APPROVED, CANCELLED),
            Command.APPLY, Map.of(APPROVED, APPLIED),
            Command.APPLY_FAILED, Map.of(APPROVED, APPLY_FAILED),
            Command.CLOSE_FOR_FINAL_STATE,
                    Map.of(DRAFT, CANCELLED, PENDING_APPROVAL, CANCELLED, APPROVED, APPLY_FAILED),
            // Trả về nháp (Q-419): chỉ từ Chờ duyệt; loại yêu cầu có cho phép không kiểm ở service
            Command.RETURN_TO_DRAFT, Map.of(PENDING_APPROVAL, DRAFT)));

    private final RequestTransitionTable table = new RequestTransitionTable();

    @Test
    void BR_74_everyStatusCommandPair_followsTheCommonTable() {
        for (Command command : Command.values()) {
            for (String status : STATUSES) {
                String expected = ALLOWED.get(command).get(status);
                if (expected != null) {
                    assertThat(table.next(status, command))
                            .as(status + " + " + command)
                            .isEqualTo(expected);
                } else {
                    assertThatThrownBy(() -> table.next(status, command))
                            .as(status + " + " + command)
                            .isInstanceOfSatisfying(
                                    BusinessException.class,
                                    e -> assertThat(e.errorCode()).isEqualTo("INVALID_STATE"));
                }
            }
        }
    }

    @Test
    void FR_031_terminalStatuses_areFinal() {
        assertThat(table.isFinal(APPLIED)).isTrue();
        assertThat(table.isFinal(APPLY_FAILED)).isTrue();
        assertThat(table.isFinal(REJECTED)).isTrue();
        assertThat(table.isFinal(CANCELLED)).isTrue();
        assertThat(table.isFinal(APPROVED)).isFalse();
    }

    /**
     * Trạng thái mở rộng chỉ là đích, không là điểm xuất phát của chuyển nào (Được thay thế của 004) là trạng thái cuối:
     * {@code closeForFinalState} không làm gì với nó (FR-070 không được bị chặn). Trạng thái mở rộng còn đi tiếp không cuối.
     */
    @Test
    void FR_031_extendedTargetWithNoOutgoingTransition_isFinal() {
        ExtendedTransitions declared = new ExtendedTransitions(Map.of(
                "SUPERSEDE",
                        new ExtendedTransitions.Transition(Set.of(DRAFT, PENDING_APPROVAL, APPROVED), "SUPERSEDED"),
                "ASK", new ExtendedTransitions.Transition(Set.of(DRAFT), "AWAITING"),
                "AGREE", new ExtendedTransitions.Transition(Set.of("AWAITING"), PENDING_APPROVAL)));

        assertThat(table.isFinal("SUPERSEDED", declared)).isTrue();
        assertThat(table.isFinal("AWAITING", declared)).isFalse();
        assertThat(table.isFinal(CANCELLED, declared)).isTrue();
        assertThat(table.isFinal(APPROVED, declared)).isFalse();
        assertThat(table.isFinal("SUPERSEDED", ExtendedTransitions.NONE)).isFalse();
    }

    @Test
    void extendedTransition_onlyAsDeclaredByRequestType() {
        ExtendedTransitions declared = new ExtendedTransitions(
                Map.of("REPLACED", new ExtendedTransitions.Transition(Set.of(APPROVED), "REPLACED")));

        assertThat(table.nextExtended(APPROVED, declared, "REPLACED")).isEqualTo("REPLACED");
        assertThatThrownBy(() -> table.nextExtended(DRAFT, declared, "REPLACED"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> table.nextExtended(APPROVED, ExtendedTransitions.NONE, "REPLACED"))
                .isInstanceOf(BusinessException.class);
    }
}
