package com.eldercare.backend.resident.internal.wish.domain;

import java.time.Instant;

/** Bản Nháp tự khai (hoặc Đã hủy) của phiếu nguyện vọng khi điều trị; {@code status} là {@code DRAFT} hoặc {@code DISCARDED}. */
public record TreatmentWishDraft(
        long id,
        long residentId,
        String status,
        TreatmentDraftContent content,
        long recordedBy,
        Instant recordedAt,
        DraftDiscard discard) {}
