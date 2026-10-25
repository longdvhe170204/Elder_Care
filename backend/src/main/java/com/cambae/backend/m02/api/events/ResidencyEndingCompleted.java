package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Hồ sơ kết thúc lưu trú đã Hoàn tất (FR-033a, FR-072). {@code endDate} là ngày kết thúc thực tế. */
public record ResidencyEndingCompleted(long residencyEndingId, long residentId, LocalDate endDate) {}
