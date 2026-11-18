package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWishDraft;
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
        WishStatus status,

        @Schema(description = "Người lập (Hành chính với bản Nháp tự khai, Q-412)")
        String recordedBy,

        @Schema(description = "Bác sĩ, Điều dưỡng kiểm chứng bản Nháp (Q-412)")
        String verifiedBy,

        Instant verifiedAt,
        DraftDiscardBody discard) {

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
                wish.status(),
                service.actorName(wish.recordedBy()),
                wish.verifiedBy() == null ? null : service.actorName(wish.verifiedBy()),
                wish.verifiedAt(),
                null);
    }

    /** Bản Nháp tự khai (Q-412, FR-023n): chưa có nhận định tự quyết, nguồn ghi khi kiểm chứng. */
    public static EndOfLifeWishBody ofDraft(EndOfLifeWishDraft draft, EndOfLifeWishService service) {
        EndOfLifeDraftContent c = draft.content();
        return new EndOfLifeWishBody(
                draft.id(),
                c.choice(),
                c.firstContact(),
                c.note(),
                null,
                c.signerType() == null ? null : service.signerName(c.signerFamilyMemberId(), draft.residentId()),
                c.signerType(),
                c.representativeSelection(),
                c.otherOpinions().stream()
                        .map(o -> new OtherOpinionBody(
                                o.source(), o.content(), service.actorName(draft.recordedBy()), draft.recordedAt()))
                        .toList(),
                c.signedAt(),
                null,
                WishStatus.valueOf(draft.status()),
                service.actorName(draft.recordedBy()),
                null,
                null,
                DraftDiscardBody.of(draft.discard(), service::actorName));
    }
}
