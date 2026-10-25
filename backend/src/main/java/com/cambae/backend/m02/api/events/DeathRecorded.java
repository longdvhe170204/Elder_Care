package com.cambae.backend.m02.api.events;

import java.time.Instant;

/** Bản ghi qua đời đã lập; 017, 018, 012 nghe (FR-033a, FR-070). */
public record DeathRecorded(long residentId, Instant at, long recordId) {}
