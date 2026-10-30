package com.eldercare.backend.resident.api.dto;

/** Lệnh thêm dị nguyên, 011 dùng khi Bác sĩ thêm thành phần thực phẩm (011 FR-003). */
public record CreateAllergen(String name, AllergenGroup group) {}
