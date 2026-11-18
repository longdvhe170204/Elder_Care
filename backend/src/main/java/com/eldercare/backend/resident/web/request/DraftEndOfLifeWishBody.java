package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeDraftContent;
import com.eldercare.backend.resident.web.request.RecordEndOfLifeWishBody.CourtDecisionBody;
import com.eldercare.backend.resident.web.request.RecordEndOfLifeWishBody.OtherOpinionBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Thân lệnh lập, sửa bản Nháp tự khai của phiếu nguyện vọng cuối đời (contracts/api-001.yaml, PUT
 * {@code /residents/{id}/end-of-life-wish-draft}; Q-412, FR-023n): không có nhận định tự quyết (chỉ Bác sĩ, BR-M01-15).
 */
@Schema(name = "EndOfLifeWishDraftBody")
public record DraftEndOfLifeWishBody(
        WishChoice choice,
        String firstContact,
        String note,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        WishSignerType respondentType,

        Long respondentFamilyMemberId,
        String relationship,
        WishSignerType signerType,
        Long signerFamilyMemberId,
        RepresentativeSelection representativeSelection,
        CourtDecisionBody courtDecision,
        List<OtherOpinionBody> otherOpinions,
        Instant signedAt,
        Long signedCopyFileId) {

    public EndOfLifeDraftContent content() {
        return new EndOfLifeDraftContent(
                choice,
                firstContact,
                note,
                respondentType,
                respondentFamilyMemberId,
                relationship,
                signerType,
                signerFamilyMemberId,
                representativeSelection,
                courtDecision == null
                        ? null
                        : new CourtDecisionRef(
                                courtDecision.number(),
                                courtDecision.date(),
                                courtDecision.court(),
                                courtDecision.fileId()),
                otherOpinions == null
                        ? List.of()
                        : otherOpinions.stream()
                                .map(o -> new WishOpinion(o.source(), o.content()))
                                .toList(),
                signedAt,
                signedCopyFileId);
    }
}
