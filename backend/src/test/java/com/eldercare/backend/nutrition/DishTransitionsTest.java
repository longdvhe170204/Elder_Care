package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.catalog.domain.DishCommand;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions.Outcome;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 011 T008: bảng trạng thái món ăn của spec (FR-004a, Q-308), kiểm bằng bảng. */
class DishTransitionsTest {

    @ParameterizedTest(name = "{0} + {2} (bản sửa chờ: {1}) → {3}")
    @CsvSource({
        "DRAFT, false, SUBMIT, PENDING_CONFIRMATION",
        "PENDING_CONFIRMATION, false, WITHDRAW, DRAFT",
        "PENDING_CONFIRMATION, false, CONFIRM, ACTIVE",
        "PENDING_CONFIRMATION, false, RETURN, DRAFT",
        "DRAFT, false, CANCEL, CANCELLED",
        "ACTIVE, false, SUBMIT_REVISION, ACTIVE",
        "ACTIVE, true, CONFIRM_REVISION, ACTIVE",
        "ACTIVE, true, RETURN_REVISION, ACTIVE",
        "ACTIVE, true, WITHDRAW_REVISION, ACTIVE",
        "ACTIVE, false, DEACTIVATE, INACTIVE",
        "ACTIVE, true, DEACTIVATE, INACTIVE"
    })
    void FR_004a_allowedTransitions(DishStatus from, boolean pending, DishCommand command, DishStatus to) {
        assertThat(DishTransitions.apply(from, pending, command)).isEqualTo(Outcome.to(to));
    }

    @ParameterizedTest(name = "{0} + {2} (bản sửa chờ: {1}) bị chặn {3}")
    @CsvSource({
        "DRAFT, false, CONFIRM, STATE_CHANGED",
        "DRAFT, false, DEACTIVATE, STATE_CHANGED",
        "PENDING_CONFIRMATION, false, SUBMIT, STATE_CHANGED",
        "PENDING_CONFIRMATION, false, CANCEL, STATE_CHANGED",
        "ACTIVE, false, CANCEL, STATE_CHANGED",
        "ACTIVE, false, CONFIRM, STATE_CHANGED",
        "ACTIVE, true, SUBMIT_REVISION, REVISION_EXISTS",
        "ACTIVE, false, CONFIRM_REVISION, STATE_CHANGED",
        "ACTIVE, false, WITHDRAW_REVISION, STATE_CHANGED",
        "INACTIVE, false, SUBMIT_REVISION, STATE_CHANGED",
        "INACTIVE, false, DEACTIVATE, STATE_CHANGED",
        "CANCELLED, false, SUBMIT, STATE_CHANGED"
    })
    void FR_004a_blockedTransitions(DishStatus from, boolean pending, DishCommand command, String errorCode) {
        assertThat(DishTransitions.apply(from, pending, command)).isEqualTo(Outcome.blocked(errorCode));
    }

    @ParameterizedTest(name = "{0}: path {1}, bếp {2}, Bác sĩ {3}, lý do {4}")
    @CsvSource({
        "SUBMIT, submit, true, false, false",
        "WITHDRAW, withdraw, true, false, false",
        "CANCEL, cancel, true, false, false",
        "CONFIRM, confirm, false, true, false",
        "RETURN, return, false, true, true",
        "SUBMIT_REVISION, submit-revision, true, false, false",
        "CONFIRM_REVISION, confirm-revision, false, true, false",
        "RETURN_REVISION, return-revision, false, true, true",
        "WITHDRAW_REVISION, withdraw-revision, true, false, false",
        "DEACTIVATE, deactivate, true, true, true"
    })
    void FR_004a_commandActorsAndReason(
            DishCommand command, String path, boolean kitchen, boolean doctor, boolean reason) {
        assertThat(DishCommand.fromPath(path)).contains(command);
        assertThat(command.byKitchen()).isEqualTo(kitchen);
        assertThat(command.byDoctor()).isEqualTo(doctor);
        assertThat(command.requiresReason()).isEqualTo(reason);
    }
}
