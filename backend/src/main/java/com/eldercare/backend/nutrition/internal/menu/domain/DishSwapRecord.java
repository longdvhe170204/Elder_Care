package com.eldercare.backend.nutrition.internal.menu.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.LocalDate;
import java.util.List;

/**
 * Một lần Đổi món ở {@code record_event} loại {@code M08.DISH_SWAP}, cha {@code M08.MENU} (thay {@code doi_mon}, nhóm 3;
 * data-model mục 5): món và món thay thế trước/sau ({@code oldDishId} rỗng là thêm, {@code newDishId} rỗng là bớt), lý do; người và lúc ở cột của bảng chung. {@code afterClosing} luôn {@code false} tới phase
 * chốt suất (FR-039).
 */
public record DishSwapRecord(
        LocalDate date,
        long mealId,
        long dietId,
        Long oldDishId,
        Long newDishId,
        List<Long> oldSubstituteDishIds,
        List<Long> substituteDishIds,
        String reason,
        boolean afterClosing) {

    public static final SharedKind<DishSwapRecord> KIND = SharedKind.of("M08.DISH_SWAP", DishSwapRecord.class);

    public DishSwapRecord {
        oldSubstituteDishIds = oldSubstituteDishIds == null ? List.of() : List.copyOf(oldSubstituteDishIds);
        substituteDishIds = substituteDishIds == null ? List.of() : List.copyOf(substituteDishIds);
    }
}
