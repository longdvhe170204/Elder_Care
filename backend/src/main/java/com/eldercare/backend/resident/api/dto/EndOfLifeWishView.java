package com.eldercare.backend.resident.api.dto;

import java.time.Instant;

/** Phiếu nguyện vọng cuối đời Hiệu lực (007 FR-023). */
public record EndOfLifeWishView(
        long id,
        long residentId,
        WishChoice choice,
        String firstContact,
        String note,
        WishSignerType signerType,
        Instant signedAt,
        WishRecordSource source) {}
