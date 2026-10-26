package com.cambae.backend.m03.api.dto;

import java.time.Instant;

/** Kiểu trao đổi AbsenceInterval, hợp đồng 003 mục 1.2–1.9 (T003). */
public record AbsenceInterval(Instant from, Instant to) {}
