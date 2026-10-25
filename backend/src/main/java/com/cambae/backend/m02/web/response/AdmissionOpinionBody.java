package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.OpinionChoice;
import com.cambae.backend.m02.internal.application.domain.OpinionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.function.LongFunction;

/** Ý kiến về việc vào ở (contracts/api-004.yaml, schema {@code AdmissionOpinion}). */
@Schema(name = "AdmissionOpinion")
public record AdmissionOpinionBody(
        long id,
        OpinionChoice opinion,
        OpinionSourceBody source,
        String note,
        String recordedBy,
        Instant at,
        OpinionStatus status) {

    public static AdmissionOpinionBody of(AdmissionOpinion opinion, LongFunction<String> names) {
        return new AdmissionOpinionBody(
                opinion.id(),
                opinion.opinion(),
                OpinionSourceBody.of(opinion.source()),
                opinion.note(),
                names.apply(opinion.recordedBy()),
                opinion.recordedAt(),
                opinion.status());
    }
}
