package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.api.dto.CourtDecisionRef;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.RecordEndOfLifeWish;
import com.cambae.backend.m01.api.dto.RepresentativeSelection;
import com.cambae.backend.m01.api.dto.SelfDeterminationAssessment;
import com.cambae.backend.m01.api.dto.WishChoice;
import com.cambae.backend.m01.api.dto.WishOpinion;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.api.dto.WishSignerType;
import com.cambae.backend.m01.web.response.SelfDeterminationAssessmentBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Thân lệnh ghi phiên bản phiếu nguyện vọng cuối đời (contracts/api-001.yaml, POST
 * {@code /residents/{id}/end-of-life-wishes}; FR-023a → FR-023c, FR-023g). Nhận định, người ký, quyết định của Tòa án,
 * bản ký không gắn {@code @NotNull} để thiếu thì trả 422 với mã lỗi nghiệp vụ thay vì 400.
 */
public record RecordEndOfLifeWishBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        WishChoice choice,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String firstContact,
        String note,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        WishSignerType respondentType,

        Long respondentFamilyMemberId,
        String relationship,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long assessingDoctorId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        SelfDeterminationAssessmentBody selfDeterminationAssessment,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        WishSignerType signerType,

        Long signerFamilyMemberId,

        @Schema(description = "Bắt buộc khi PATIENT_REPRESENTATIVE")
        RepresentativeSelection representativeSelection,

        CourtDecisionBody courtDecision,

        @Schema(description = "Ý kiến khác với lựa chọn, mỗi ý kiến một mục có nguồn (FR-023c, FR-023g)")
        List<OtherOpinionBody> otherOpinions,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant signedAt,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long signedCopyFileId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        WishRecordSource source) {

    /** Quyết định của Tòa án (schema {@code CourtDecision}); trường thiếu bị từ chối ở dịch vụ. */
    @Schema(name = "CourtDecision")
    public record CourtDecisionBody(String number, java.time.LocalDate date, String court, Long fileId) {}

    @Schema(name = "RecordEndOfLifeWishOpinion")
    public record OtherOpinionBody(@NotNull OpinionSource source, String content) {}

    public RecordEndOfLifeWish command(long residentId) {
        SelfDeterminationAssessment assessment = selfDeterminationAssessment == null
                ? null
                : new SelfDeterminationAssessment(
                        selfDeterminationAssessment.canSelfDetermine(),
                        selfDeterminationAssessment.supportMethod(),
                        selfDeterminationAssessment.reviewDate());
        CourtDecisionRef decision = courtDecision == null
                ? null
                : new CourtDecisionRef(
                        courtDecision.number(), courtDecision.date(), courtDecision.court(), courtDecision.fileId());
        List<WishOpinion> opinions = otherOpinions == null
                ? List.of()
                : otherOpinions.stream()
                        .map(o -> new WishOpinion(o.source(), o.content()))
                        .toList();
        return new RecordEndOfLifeWish(
                residentId,
                choice,
                firstContact,
                note,
                respondentType,
                respondentFamilyMemberId,
                relationship,
                assessingDoctorId,
                assessment,
                signerType,
                signerFamilyMemberId,
                representativeSelection,
                decision,
                opinions,
                signedAt,
                signedCopyFileId);
    }
}
