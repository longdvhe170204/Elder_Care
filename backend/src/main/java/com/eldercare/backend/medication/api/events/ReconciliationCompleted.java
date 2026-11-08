package com.eldercare.backend.medication.api.events;

import java.time.Instant;

/**
 * Phiếu đối chiếu thuốc được xác nhận (FR-036, research D-13): hạn đối chiếu đã gỡ; 007 nghe để đóng cảnh báo
 * "chưa đối chiếu thuốc" của người đó (research S-09), không để 006 gọi {@code incident.api} vì incident đã phụ thuộc
 * medication.
 */
public record ReconciliationCompleted(long residentId, Instant occurredAt) {}
