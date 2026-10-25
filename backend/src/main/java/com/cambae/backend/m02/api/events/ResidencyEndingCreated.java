package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Hồ sơ kết thúc lưu trú đã lập; 010, 017, 018, 012 nghe (FR-033a, FR-066). */
public record ResidencyEndingCreated(long residencyEndingId, long residentId, LocalDate expectedEndDate) {}
