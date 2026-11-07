package com.eldercare.backend.nutrition.api.dto;

import java.time.LocalDate;

/** Món của chế độ ăn mặc định theo ngày, bữa (thực đơn chung, FR-028); không có thông tin cá nhân. */
public record DishByMealView(LocalDate date, long mealId, String mealName, long dishId, String dishName) {}
