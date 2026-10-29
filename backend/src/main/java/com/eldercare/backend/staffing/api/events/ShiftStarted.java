package com.eldercare.backend.staffing.api.events;

import java.time.Instant;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record ShiftStarted(long shiftId, Long floorAreaId, Instant occurredAt) {

    public String eventKey() {
        return "SHIFT_STARTED:" + shiftId + ":" + occurredAt;
    }
}
