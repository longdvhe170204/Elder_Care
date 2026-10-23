package com.cambae.backend.m01.api.dto;

import java.time.Instant;
import java.util.Optional;

/** Khoảng thời gian người cao tuổi ở một trạng thái, lấy từ lịch sử trạng thái (003 FR-027, FR-036); {@code to} rỗng nếu còn đang ở. */
public record StatusPeriod(ResidentStatus status, Instant from, Optional<Instant> to) {}
