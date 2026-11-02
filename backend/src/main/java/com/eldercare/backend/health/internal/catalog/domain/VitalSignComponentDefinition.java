package com.eldercare.backend.health.internal.catalog.domain;
/** Thành phần danh mục FR-001; mã duy nhất trong từng chỉ số. */
public record VitalSignComponentDefinition(String code, String name, int decimalPlaces) {}
