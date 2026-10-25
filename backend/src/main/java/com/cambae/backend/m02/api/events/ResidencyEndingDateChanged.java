package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Ngày kết thúc dự kiến của hồ sơ kết thúc đã đổi (FR-066). */
public record ResidencyEndingDateChanged(long residencyEndingId, long residentId, LocalDate expectedEndDate) {}
