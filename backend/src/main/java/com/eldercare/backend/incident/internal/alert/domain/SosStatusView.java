package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;
import java.util.List;

/** Trạng thái cảnh báo SOS cho kênh người cao tuổi (FR-047f): {@code SENT}, {@code RECEIVED} kèm họ tên, {@code ARRIVED}. */
public record SosStatusView(
        long pressId,
        long alertId,
        String status,
        String receiverName,
        Instant arrivedAt,
        String facilityPhone,
        boolean sendLocation,
        int locationIntervalSeconds,
        Instant sentAt,
        List<String> helperNames) {
    public SosStatusView {
        helperNames = helperNames == null ? List.of() : List.copyOf(helperNames);
    }

    public SosStatusView(
            long pressId,
            long alertId,
            String status,
            String receiverName,
            Instant arrivedAt,
            String facilityPhone,
            boolean sendLocation,
            int locationIntervalSeconds) {
        this(
                pressId,
                alertId,
                status,
                receiverName,
                arrivedAt,
                facilityPhone,
                sendLocation,
                locationIntervalSeconds,
                null,
                List.of());
    }
}
