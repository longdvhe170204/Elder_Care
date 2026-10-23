package com.cambae.backend.m07.api.dto;

/**
 * Tóm tắt thông tin thuốc (ThuocTomTat).
 * Thuộc nhóm thông tin SUC_KHOE, có thể bị ẩn tùy theo quyền xem của người dùng.
 */
public record MedicationSummary(
        long medicationId, String name, String activeIngredient, String strength, String dosageForm) {}
