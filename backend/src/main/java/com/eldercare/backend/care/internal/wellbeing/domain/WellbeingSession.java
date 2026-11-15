package com.eldercare.backend.care.internal.wellbeing.domain;

import java.time.Instant;

/** Thông tin buổi cần cho tổng hợp FR-053. */
public record WellbeingSession(long id, Instant startsAt, boolean groupActivity) {}
