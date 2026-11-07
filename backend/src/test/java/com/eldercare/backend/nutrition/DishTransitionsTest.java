package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.catalog.domain.DishCommand;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions.Outcome;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

/** 011 T008: bảng trạng thái món ăn của spec (FR-004a, Q-308), kiểm bằng bảng. */
class DishTransitionsTest {

    @Test
    void FR_004a_allowedTransitions() {
        // từ, bản sửa chờ, lệnh, tới
        TableRows.csv(
                r -> assertThat(DishTransitions.apply(
                                DishStatus.valueOf(r[0]), Boolean.parseBoolean(r[1]), DishCommand.valueOf(r[2])))
                        .isEqualTo(Outcome.to(DishStatus.valueOf(r[3]))),
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
                "ACTIVE, true, DEACTIVATE, INACTIVE");
    }

    @Test
    void FR_004a_blockedTransitions() {
        // từ, bản sửa chờ, lệnh, mã lỗi
        TableRows.csv(
                r -> assertThat(DishTransitions.apply(
                                DishStatus.valueOf(r[0]), Boolean.parseBoolean(r[1]), DishCommand.valueOf(r[2])))
                        .isEqualTo(Outcome.blocked(r[3])),
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
                "CANCELLED, false, SUBMIT, STATE_CHANGED");
    }

    @Test
    void FR_004a_commandActorsAndReason() {
        // lệnh, path, bếp, Bác sĩ, lý do
        TableRows.csv(
                r -> {
                    var command = DishCommand.valueOf(r[0]);
                    assertThat(DishCommand.fromPath(r[1])).contains(command);
                    assertThat(command.byKitchen()).isEqualTo(Boolean.parseBoolean(r[2]));
                    assertThat(command.byDoctor()).isEqualTo(Boolean.parseBoolean(r[3]));
                    assertThat(command.requiresReason()).isEqualTo(Boolean.parseBoolean(r[4]));
                },
                "SUBMIT, submit, true, false, false",
                "WITHDRAW, withdraw, true, false, false",
                "CANCEL, cancel, true, false, false",
                "CONFIRM, confirm, false, true, false",
                "RETURN, return, false, true, true",
                "SUBMIT_REVISION, submit-revision, true, false, false",
                "CONFIRM_REVISION, confirm-revision, false, true, false",
                "RETURN_REVISION, return-revision, false, true, true",
                "WITHDRAW_REVISION, withdraw-revision, true, false, false",
                "DEACTIVATE, deactivate, true, true, true");
    }
}
