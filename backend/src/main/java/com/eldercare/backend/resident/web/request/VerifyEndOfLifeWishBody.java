package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.web.request.RecordEndOfLifeWishBody.CourtDecisionBody;
import com.eldercare.backend.resident.web.request.RecordEndOfLifeWishBody.OtherOpinionBody;
import com.eldercare.backend.resident.web.response.SelfDeterminationAssessmentBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Thân lệnh kiểm chứng bản Nháp phiếu nguyện vọng cuối đời (contracts/api-001.yaml, POST
 * {@code /residents/{id}/end-of-life-wish-draft/verify}; Q-412, FR-023p): như thân lệnh ghi phiên bản, không có
 * {@code source} (luôn {@code ADMISSION}).
 */
@Schema(name = "EndOfLifeWishVerifyBody")
public record VerifyEndOfLifeWishBody(
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
        RepresentativeSelection representativeSelection,
        CourtDecisionBody courtDecision,
        List<OtherOpinionBody> otherOpinions,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant signedAt,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long signedCopyFileId) {

    public RecordEndOfLifeWish command(long residentId) {
        return new RecordEndOfLifeWishBody(
                        choice,
                        firstContact,
                        note,
                        respondentType,
                        respondentFamilyMemberId,
                        relationship,
                        assessingDoctorId,
                        selfDeterminationAssessment,
                        signerType,
                        signerFamilyMemberId,
                        representativeSelection,
                        courtDecision,
                        otherOpinions,
                        signedAt,
                        signedCopyFileId,
                        WishRecordSource.ADMISSION)
                .command(residentId);
    }
}
