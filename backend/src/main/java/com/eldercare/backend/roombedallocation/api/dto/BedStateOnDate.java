package com.eldercare.backend.roombedallocation.api.dto;

import java.time.Instant;

/**
 * Kiểu trao đổi BedStateOnDate, hợp đồng 003 mục 1.2–1.9 (T003). {@code roomId}, {@code holdReason}
 * ({@code WAITING_RECORD}, {@code ABSENT_RESIDENT}; rỗng khi không ở Đang giữ chỗ) cho 016 FR-020, FR-024 (luật "Đổi api",
 * chủ dự án duyệt 2026-10-27).
 */
public record BedStateOnDate(long bedId, long floorId, BedStatus state, Instant at, long roomId, String holdReason) {}
