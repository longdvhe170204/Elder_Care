package com.eldercare.backend.roombedallocation.internal.daycare.domain;

import com.eldercare.backend.roombedallocation.api.dto.DayCareSession;
import java.time.LocalTime;

/** Ảnh đọc khung giờ, giữ version riêng khỏi schema HTTP. */
public record DayCareSessionState(
        long id, String name, LocalTime startTime, LocalTime endTime, String status, int version) {
    public DayCareSession view() {
        return new DayCareSession(id, name, startTime, endTime, "ACTIVE".equals(status));
    }
}
