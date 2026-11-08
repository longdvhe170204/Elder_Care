package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/** Khoảng người nghi nhiễm vắng mặt trong khoảng truy vết (FR-060, 003 FR-036), đã cắt theo khoảng truy vết. */
public record AbsencePeriod(String status, Instant from, Instant to) {}
