package com.cambae.backend.m02.api.events;

import java.time.Instant;

/** Lượt vắng đã mở; 001, 006, 011, 010 nghe (FR-048). */
public record AbsenceOpened(long absenceId, long residentId, Instant at) {}
