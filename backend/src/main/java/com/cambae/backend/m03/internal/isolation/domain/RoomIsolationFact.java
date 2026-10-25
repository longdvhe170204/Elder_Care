package com.cambae.backend.m03.internal.isolation.domain;

import java.time.Instant;

/** Một lần đặt/gỡ, không có lệnh sửa hoặc xóa lịch sử. */
public record RoomIsolationFact(Action action, Instant occurredAt, long actorId, String reason, Long incidentId) {
    public enum Action {
        ISOLATE,
        LIFT
    }
}
