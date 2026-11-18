package com.eldercare.backend.resident.internal.wish.domain;

import java.time.Instant;

/** Bản Nháp tự khai (hoặc Đã hủy) của phiếu nguyện vọng cuối đời; {@code status} là {@code DRAFT} hoặc {@code DISCARDED}. */
public record EndOfLifeWishDraft(
        long id,
        long residentId,
        String status,
        EndOfLifeDraftContent content,
        long recordedBy,
        Instant recordedAt,
        DraftDiscard discard) {}
