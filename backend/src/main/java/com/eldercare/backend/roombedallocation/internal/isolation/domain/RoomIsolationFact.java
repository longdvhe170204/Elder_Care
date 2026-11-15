package com.eldercare.backend.roombedallocation.internal.isolation.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.time.Instant;

/** Một lần đặt/gỡ, không có lệnh sửa hoặc xóa lịch sử. */
public record RoomIsolationFact(Action action, Instant occurredAt, long actorId, String reason, Long incidentId) {
    /** Dòng {@code status_history} của lịch sử cách ly phòng (R-15, gộp 2026-11-13; thay {@code room_isolation_history}). */
    public static final HistoryTrack<Void> TRACK = new HistoryTrack<>("M03.ROOM", "ISOLATION", Void.class);

    public static final String INCIDENT_BASIS = "M07.INCIDENT";

    public enum Action {
        ISOLATE,
        LIFT
    }
}
