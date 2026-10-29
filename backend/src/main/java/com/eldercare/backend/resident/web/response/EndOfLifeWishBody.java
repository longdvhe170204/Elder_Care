package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.WishStatus;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
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
