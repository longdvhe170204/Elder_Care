package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.internal.daycare.domain.SessionCapacityRow;
import java.time.LocalDate;

/** Số đăng ký và số chỗ còn lại; số âm thể hiện vượt sức chứa. */
public record DayCareSessionCapacity(
        LocalDate date, long sessionId, String sessionName, int registrations, int capacity, int remaining) {
    public static DayCareSessionCapacity of(SessionCapacityRow value) {
        return new DayCareSessionCapacity(
                value.date(),
                value.sessionId(),
                value.sessionName(),
                value.registrations(),
                value.capacity(),
                value.remaining());
    }
}
