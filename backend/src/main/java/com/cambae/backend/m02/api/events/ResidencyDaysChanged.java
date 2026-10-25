package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Kết quả ngày lưu trú đổi trong khoảng; 010 tính lại khoản (FR-052, FR-056, FR-061). {@code cause} là mã nguyên nhân. */
public record ResidencyDaysChanged(long residentId, LocalDate fromDate, LocalDate toDate, String cause) {}
