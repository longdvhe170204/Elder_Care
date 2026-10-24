package com.cambae.backend.m03.internal.daycare.domain;

import com.cambae.backend.m03.api.dto.DayCareSession;
import java.time.LocalTime;

/** Ảnh đọc khung giờ, giữ version riêng khỏi schema HTTP. */
public record DayCareSessionState(
        long id, String name, LocalTime startTime, LocalTime endTime, String status, int version) {
    public DayCareSession view() {
        return new DayCareSession(id, name, startTime, endTime, "ACTIVE".equals(status));
    }
}
