package com.eldercare.backend.nutrition.api.dto;

import java.time.LocalDate;

/**
 * Suất ăn dự kiến của một người cao tuổi (FR-063, research M-16): chỉ ngày, bữa, tên món, cờ món thay thế; không có chế
 * độ ăn, kết cấu, dị ứng (Q-367).
 */
public record ExpectedPortionView(
        LocalDate date, long mealId, String mealName, String dishName, boolean isSubstitute) {}
