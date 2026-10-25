package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Hồ sơ kết thúc lưu trú đã hủy (FR-066). {@code expectedEndDate} là ngày dự kiến trước khi hủy. */
public record ResidencyEndingCancelled(long residencyEndingId, long residentId, LocalDate expectedEndDate) {}
