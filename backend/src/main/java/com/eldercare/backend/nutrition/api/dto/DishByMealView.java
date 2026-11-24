package com.eldercare.backend.nutrition.api.dto;

import java.time.LocalDate;

/**
 * Một món của thực đơn chung theo ngày, bữa (FR-028, Q-445): tên, mô tả, url ảnh ({@code /api/v1/files/{id}}), giờ bữa dự
 * kiến; không có thông tin cá nhân.
 */
public record DishByMealView(
        LocalDate date,
        long mealId,
        String mealName,
        String expectedTime,
        long dishId,
        String dishName,
        String description,
        String imageUrl) {}
