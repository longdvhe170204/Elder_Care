package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import java.time.LocalDate;
import java.util.List;

/** Thực đơn chung cho 012 (FR-028, module-interfaces mục 1.3): món của chế độ ăn mặc định, không có thông tin cá nhân. */
public interface GeneralMenuApi {

    List<DishByMealView> generalMenu(LocalDate from, LocalDate to);

    List<String> dietNames();
}
