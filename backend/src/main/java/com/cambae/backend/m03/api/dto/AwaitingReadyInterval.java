package com.cambae.backend.m03.api.dto;

import java.time.Instant;

/**
 * Kiểu trao đổi AwaitingReadyInterval, hợp đồng 003 mục 1.2–1.9 (T003). {@code from}, {@code to} đã cắt theo khoảng tra
 * cứu; {@code startedAt} là lúc giường vào Chờ sẵn sàng, không cắt, cho 016 FR-033, Q-390 (luật "Đổi api", chủ dự án duyệt
 * 2026-10-27).
 */
public record AwaitingReadyInterval(
        long bedId, long floorId, Instant from, Instant to, Instant deadline, Instant startedAt) {}
