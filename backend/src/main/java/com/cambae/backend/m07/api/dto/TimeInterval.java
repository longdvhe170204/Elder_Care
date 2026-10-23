package com.cambae.backend.m07.api.dto;

import java.time.Instant;

/**
 * Khoảng thời gian từ mốc đến mốc (KhoangThoiGian).
 */
public record TimeInterval(Instant from, Instant to) {}
