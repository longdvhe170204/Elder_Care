package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.RepresentativeSelection;
import com.cambae.backend.m01.api.dto.WishChoice;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.api.dto.WishSignerType;
import com.cambae.backend.m01.internal.wish.domain.EndOfLifeWish;
import com.cambae.backend.m01.internal.wish.domain.WishStatus;
import com.cambae.backend.m01.internal.wish.service.EndOfLifeWishService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Một phiên bản phiếu nguyện vọng cuối đời (contracts/api-001.yaml, schema {@code EndOfLifeWish}). */
@Schema(name = "EndOfLifeWish")
public record EndOfLifeWishBody(
        long id,
        WishChoice choice,
        String firstContact,
        String note,
        SelfDeterminationAssessmentBody selfDeterminationAssessment,
        String signer,
        WishSignerType signerType,
        RepresentativeSelection representativeSelection,
        List<OtherOpinionBody> otherOpinions,
        Instant signedAt,
        WishRecordSource source,
        WishStatus status) {

    @Schema(name = "EndOfLifeWishOpinion")
    public record OtherOpinionBody(OpinionSource source, String content, String recordedBy, Instant at) {}

    public static EndOfLifeWishBody of(EndOfLifeWish wish, EndOfLifeWishService service) {
        return new EndOfLifeWishBody(
                wish.id(),
                wish.choice(),
                wish.firstContact(),
                wish.note(),
                new SelfDeterminationAssessmentBody(
                        wish.canSelfDetermine(), wish.assessedAt(), wish.supportMethod(), wish.reviewDate()),
                service.signerName(wish),
                wish.signerType(),
                wish.representativeSelection(),
                wish.otherOpinions().stream()
                        .map(o -> new OtherOpinionBody(
                                o.source(), o.content(), service.actorName(o.recordedBy()), o.at()))
                        .toList(),
                wish.signedAt(),
                wish.source(),
                wish.status());
    }
}
