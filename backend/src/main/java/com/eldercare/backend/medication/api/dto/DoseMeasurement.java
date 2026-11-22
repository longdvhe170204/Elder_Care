package com.eldercare.backend.medication.api.dto;

import java.math.BigDecimal;

/** Chỉ số đo trước khi dùng thuốc (BR-M07-26): mã chỉ số trong danh mục chỉ số và giá trị đo. */
public record DoseMeasurement(String code, BigDecimal value) {}
