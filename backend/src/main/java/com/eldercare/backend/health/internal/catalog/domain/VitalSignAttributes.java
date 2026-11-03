package com.eldercare.backend.health.internal.catalog.domain;

/** Thuộc tính danh mục chỉ số, FR-001; không suy diễn cờ sinh hiệu từ tên. */
public record VitalSignAttributes(String unit, boolean vitalSign) {}
