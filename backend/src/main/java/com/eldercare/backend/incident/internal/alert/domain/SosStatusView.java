package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/** Trạng thái cảnh báo SOS cho kênh người cao tuổi (FR-047f): {@code SENT}, {@code RECEIVED} kèm họ tên, {@code ARRIVED}. */
public record SosStatusView(
        long pressId, long alertId, String status, String receiverName, Instant arrivedAt, String facilityPhone) {}
