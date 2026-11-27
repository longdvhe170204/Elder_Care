package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.catalog.domain.DishCommand;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions.Outcome;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

/** 011 T048: bảng trạng thái món ăn của spec (FR-004a, Q-445), kiểm bằng bảng. */
class DishTransitionsTest {

    @Test
    void FR_004a_allowedTransitions() {
        // từ, lệnh, tới
        TableRows.csv(
                r -> assertThat(DishTransitions.apply(DishStatus.valueOf(r[0]), DishCommand.valueOf(r[1])))
                        .isEqualTo(Outcome.to(DishStatus.valueOf(r[2]))),
                "DRAFT, ACTIVATE, ACTIVE",
                "DRAFT, CANCEL, CANCELLED",
                "ACTIVE, DEACTIVATE, INACTIVE");
    }

    @Test
    void FR_004a_blockedTransitions() {
        // từ, lệnh, mã lỗi
        TableRows.csv(
                r -> assertThat(DishTransitions.apply(DishStatus.valueOf(r[0]), DishCommand.valueOf(r[1])))
                        .isEqualTo(Outcome.blocked(r[2])),
                "ACTIVE, ACTIVATE, STATE_CHANGED",
                "ACTIVE, CANCEL, STATE_CHANGED",
                "DRAFT, DEACTIVATE, STATE_CHANGED",
                "INACTIVE, ACTIVATE, STATE_CHANGED",
                "INACTIVE, DEACTIVATE, STATE_CHANGED",
                "CANCELLED, ACTIVATE, STATE_CHANGED");
    }

    @Test
    void FR_004a_commandPathsAndReason() {
        // lệnh, path, lý do
        TableRows.csv(
                r -> {
                    DishCommand c = DishCommand.valueOf(r[0]);
                    assertThat(c.path()).isEqualTo(r[1]);
                    assertThat(DishCommand.fromPath(r[1])).contains(c);
                    assertThat(c.requiresReason()).isEqualTo(Boolean.parseBoolean(r[2]));
                },
                "ACTIVATE, activate, false",
                "CANCEL, cancel, false",
                "DEACTIVATE, deactivate, true");
        assertThat(DishCommand.fromPath("confirm")).isEmpty();
    }
}
