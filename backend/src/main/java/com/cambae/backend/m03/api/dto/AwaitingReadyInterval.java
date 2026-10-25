package com.cambae.backend.m03.api.dto;

import java.time.Instant;

/** Kiểu trao đổi AwaitingReadyInterval, hợp đồng 003 mục 1.2–1.9 (T003). */
public record AwaitingReadyInterval(long bedId, long floorId, Instant from, Instant to, Instant deadline) {}
