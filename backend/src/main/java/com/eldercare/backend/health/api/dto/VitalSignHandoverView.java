package com.eldercare.backend.health.api.dto;

import java.time.Instant;

/** Chỉ số vượt ngưỡng đã xác nhận, giá trị sau đính chính. */
public record VitalSignHandoverView(long id, long residentId, Long roomId, String description, Instant measuredAt) {}
