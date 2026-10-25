package com.cambae.backend.m02.api.events;

import java.time.Instant;

/** Mốc dự kiến trở lại đã gia hạn (FR-049). */
public record AbsenceExtended(long absenceId, long residentId, Instant expectedReturnAt) {}
