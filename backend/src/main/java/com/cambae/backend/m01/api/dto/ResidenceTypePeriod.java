package com.cambae.backend.m01.api.dto;

import java.time.LocalDate;
import java.util.Optional;

/** Một khoảng ngày áp dụng một loại lưu trú trong hợp đồng (FR-052); {@code to} rỗng nếu còn hiệu lực. */
public record ResidenceTypePeriod(ResidenceType type, LocalDate from, Optional<LocalDate> to) {}
