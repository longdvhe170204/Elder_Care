package com.cambae.backend.m03.api.dto;

import java.time.Instant;

/** Phân bổ chờ bác sĩ xác nhận vùng cách ly, 003 mục 1.9. */
public record AllocationHandoverView(long id, long residentId, Long roomId, String description, Instant createdAt) {}
