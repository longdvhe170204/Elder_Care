package com.cambae.backend.m07.api.spi;

import java.time.Instant;

/**
 * Thông tin ca trực trả về từ module M09 (CaView).
 */
public record ShiftView(long shiftId, long floorAreaId, Instant startTime, Instant endTime, Long shiftLeadId) {}
