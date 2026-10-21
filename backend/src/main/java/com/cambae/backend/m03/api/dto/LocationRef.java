package com.cambae.backend.m03.api.dto;

/** Tham chiếu một cấp của cơ cấu vị trí (003 FR-001, FR-005). */
public record LocationRef(Type type, long id) {
    public enum Type {
        AREA,
        FLOOR,
        ROOM,
        BED
    }
}
