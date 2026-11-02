package com.eldercare.backend.admission.internal.application.domain;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.time.Instant;

/** Ý kiến về việc vào ở (data-model mục 3, dòng {@code OPINION} của {@code admission_record}); nội dung bất biến. */
public record AdmissionOpinion(
        long id,
        long applicationId,
        OpinionChoice opinion,
        OpinionSource source,
        String note,
        long recordedBy,
        Instant recordedAt,
        OpinionStatus status) {}
