package com.eldercare.backend.nutrition.internal.menu.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Một lần Đổi món ở {@code record_event} loại {@code M08.DISH_SWAP}, cha {@code M08.MENU} (nhóm 3; data-model mục 13.3):
 * món trước, món sau ({@code oldDishId} rỗng là thêm, {@code newDishId} rỗng là bớt), lý do; người và lúc ở cột của bảng
 * chung. Bỏ qua {@code dietId}, món thay thế của bản ghi có trước Q-445.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DishSwapRecord(
        LocalDate date, long mealId, Long oldDishId, Long newDishId, String reason, boolean afterClosing) {

    public static final SharedKind<DishSwapRecord> KIND = SharedKind.of("M08.DISH_SWAP", DishSwapRecord.class);
}
