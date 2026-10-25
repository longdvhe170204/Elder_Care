package com.cambae.backend.m02.internal.application.domain;

import com.cambae.backend.m01.api.dto.OpinionSource;
import java.time.Instant;

/** Ý kiến về việc vào ở (data-model mục 3, bảng {@code admission_opinion}); nội dung bất biến. */
public record AdmissionOpinion(
        long id,
        long applicationId,
        OpinionChoice opinion,
        OpinionSource source,
        String note,
        long recordedBy,
        Instant recordedAt,
        OpinionStatus status) {}
