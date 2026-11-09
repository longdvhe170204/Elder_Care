package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả xác nhận lại nguyện vọng (schema {@code WishReconfirmation} của api-007; FR-050c). {@code newWish} cùng thân
 * lệnh {@code POST /residents/{id}/end-of-life-wishes} của api-001 (trừ {@code source}, 007 ghi
 * {@code CRITICAL_RECONFIRMATION}); 001 kiểm nội dung và trả 422 khi thiếu.
 */
@Schema(
        name = "WishReconfirmation",
        requiredProperties = {"contactedPerson", "channel", "contactedAt", "result"})
public record WishReconfirmationRequest(
        ContactedPerson contactedPerson,

        @Schema(allowableValues = {"PHONE_CALL", "IN_PERSON"})
        String channel,

        Instant contactedAt,

        @Schema(allowableValues = {"KEPT", "CHANGED", "UNREACHABLE"})
        String result,

        @Schema(implementation = Object.class, description = "Bắt buộc khi CHANGED")
        NewWish newWish) {

    public record ContactedPerson(Long familyMemberId, String fullName) {}

    /** Thân lệnh nguyện vọng của api-001 sau cập nhật 2026-10-18 (FR-023b, FR-023c, FR-023g). */
    public record NewWish(
            WishChoice choice,
            String firstContact,
            String note,
            WishSignerType respondentType,
            Long respondentFamilyMemberId,
            String relationship,
            Long assessingDoctorId,
            Assessment selfDeterminationAssessment,
            WishSignerType signerType,
            Long signerFamilyMemberId,
            RepresentativeSelection representativeSelection,
            CourtDecision courtDecision,
            List<Opinion> otherOpinions,
            Instant signedAt,
            Long signedCopyFileId) {

        public RecordEndOfLifeWish command(long residentId) {
            return new RecordEndOfLifeWish(
                    residentId,
                    choice,
                    firstContact,
                    note,
                    respondentType,
                    respondentFamilyMemberId,
                    relationship,
                    assessingDoctorId,
                    selfDeterminationAssessment == null
                            ? null
                            : new SelfDeterminationAssessment(
                                    selfDeterminationAssessment.canSelfDetermine(),
                                    selfDeterminationAssessment.supportMethod(),
                                    selfDeterminationAssessment.reviewDate()),
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

    public record Assessment(boolean canSelfDetermine, String supportMethod, LocalDate reviewDate) {}

    public record CourtDecision(String number, LocalDate date, String court, Long fileId) {}

    public record Opinion(OpinionSource source, String content) {}
}
