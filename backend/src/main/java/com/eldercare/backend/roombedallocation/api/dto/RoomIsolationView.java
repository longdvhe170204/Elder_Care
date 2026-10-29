package com.eldercare.backend.roombedallocation.api.dto;

import java.time.Instant;

/** Kiểu trao đổi RoomIsolationView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record RoomIsolationView(long roomId, long floorId, Instant from, Instant to) {}
