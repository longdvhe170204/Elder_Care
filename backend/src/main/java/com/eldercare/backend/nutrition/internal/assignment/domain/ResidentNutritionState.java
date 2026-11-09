package com.eldercare.backend.nutrition.internal.assignment.domain;

/**
 * Dữ liệu dòng khóa theo người cao tuổi ({@code M08.RESIDENT_NUTRITION} ở {@code resident_module_state}); chưa dùng
 * trường nào ngoài số lần khóa.
 */
public record ResidentNutritionState(long locks) {}
