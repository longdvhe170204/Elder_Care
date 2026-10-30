package com.eldercare.backend.administration.correction.api.dto;

import java.time.Instant;

/** Một bản đính chính; {@code originalRecordId} luôn là bản gốc ban đầu (FR-027). */
public record CorrectionView(
        long id,
        String recordType,
        long originalRecordId,
        int sequenceNo,
        CorrectionType type,
        Object correctedContent,
        String reason,
        long actorId,
        CreationMethod creationMethod,
        Long approvalRequestId,
        Instant recordedAt) {}
