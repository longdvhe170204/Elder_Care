package com.cambae.backend.m03.api.dto;

import java.time.Instant;

/** Kiểu trao đổi BedStateOnDate, hợp đồng 003 mục 1.2–1.9 (T003). */
public record BedStateOnDate(long bedId, long floorId, BedStatus state, Instant at) {}
