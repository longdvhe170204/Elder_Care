package com.eldercare.backend.staffing.api.events;

import java.time.Instant;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record HandoverConfirmed(long handoverId, long previousShiftId, Long nextShiftId, Instant occurredAt) {

    public String eventKey() {
        return "HANDOVER_CONFIRMED:" + handoverId + ":" + occurredAt;
    }
}
