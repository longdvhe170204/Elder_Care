package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Báo vắng bán trú đã ghi; 005, 010, 011 nghe (FR-060). */
public record DayCareAbsenceNoticeRecorded(
        long residentId, LocalDate date, long sessionId, boolean withinNoticePeriod) {}
