package com.eldercare.backend.staffing.api.events;

import java.time.Instant;

/** Sự kiện 008 mục 1.6; dịch vụ nguồn phát trong transaction, Modulith chuyển sau commit. */
public record ShiftEnded(long shiftId, Long floorAreaId, String status, Instant occurredAt) {

    public String eventKey() {
        return "SHIFT_ENDED:" + shiftId + ":" + occurredAt;
    }
}
