package com.eldercare.backend.resident.internal.wish.domain;

import java.time.Instant;

/** Người, thời điểm, lý do Hủy nháp phiếu nguyện vọng (Q-412, FR-023p). */
public record DraftDiscard(long by, Instant at, String reason) {}
