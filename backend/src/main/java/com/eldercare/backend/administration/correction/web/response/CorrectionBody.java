package com.eldercare.backend.administration.correction.web.response;

import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.administration.correction.api.CorrectionView;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import java.time.Instant;
import java.util.function.LongFunction;

public record CorrectionBody(
        long id,
        int sequenceNo,
        CorrectionType type,
        Object correctedContent,
        String reason,
        ActorBody actor,
        CreationMethod creationMethod,
        Long approvalRequestId,
        Instant recordedAt) {

    public static CorrectionBody of(CorrectionView c, LongFunction<String> names) {
        return new CorrectionBody(
                c.id(),
                c.sequenceNo(),
                c.type(),
                c.correctedContent(),
                c.reason(),
                new ActorBody(ActorType.USER, c.actorId(), names.apply(c.actorId()), null),
                c.creationMethod(),
                c.approvalRequestId(),
                c.recordedAt());
    }
}
