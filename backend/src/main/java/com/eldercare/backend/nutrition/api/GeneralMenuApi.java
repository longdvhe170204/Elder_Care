package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import java.time.LocalDate;
import java.util.List;

/**
 * Thực đơn chung đã Công bố, Đã áp dụng (FR-028, module-interfaces mục 1.9; Q-445): mọi món theo ngày, bữa, theo thứ tự;
 * không có thông tin cá nhân. Dùng cho 012 (cổng người thân) và màn thực đơn của app.
 */
public interface GeneralMenuApi {

    List<DishByMealView> generalMenu(LocalDate from, LocalDate to);
}
