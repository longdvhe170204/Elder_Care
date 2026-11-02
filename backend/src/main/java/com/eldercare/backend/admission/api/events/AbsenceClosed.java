package com.eldercare.backend.admission.api.events;

import java.time.Instant;

/** Lượt vắng đã đóng (FR-049). */
public record AbsenceClosed(long absenceId, long residentId, Instant at) {}
