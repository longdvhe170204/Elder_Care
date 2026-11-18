package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.NotifyContact;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishStatus;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Một phiên bản phiếu nguyện vọng khi điều trị (contracts/api-001.yaml, schema {@code TreatmentWish}). */
@Schema(name = "TreatmentWish")
public record TreatmentWishBody(
        long id,
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        Boolean askFamilyFirst,
        List<NotifyFamilyMemberBody> notifyFamilyMembers,
        TreatmentEscort accompaniedBy,
        String note,
        OpinionSource respondent,
        List<OtherOpinionBody> otherOpinions,
        Long signedCopyFileId,
        String recordedBy,
        Instant at,
        TreatmentWishStatus status,

        @Schema(description = "Bác sĩ, Điều dưỡng kiểm chứng bản Nháp (Q-412)")
        String verifiedBy,

        Instant verifiedAt,
        DraftDiscardBody discard) {

    /** {@code relationship} là mã {@code FamilyRelationshipType} (002 "Đổi api" 2026-10-30), web hiện nhãn tiếng Việt. */
    public record NotifyFamilyMemberBody(
            long familyMemberId, String fullName, String relationship, String phoneNumber) {}

    @Schema(name = "TreatmentWishOpinion")
    public record OtherOpinionBody(OpinionSource source, String content, String recordedBy, Instant at) {}

    public static TreatmentWishBody of(TreatmentWish wish, TreatmentWishService service) {
        return new TreatmentWishBody(
                wish.id(),
                wish.preferredHospitals(),
                wish.healthInsurance(),
                wish.askFamilyFirst(),
                service.notifyContacts(wish).stream()
                        .map(c -> new NotifyFamilyMemberBody(
                                c.familyMemberId(),
                                c.fullName(),
                                service.relationshipOf(wish, c.familyMemberId()),
                                c.phoneNumber()))
                        .toList(),
                wish.accompaniedBy(),
                wish.note(),
                wish.respondent(),
                wish.otherOpinions().stream()
                        .map(o -> new OtherOpinionBody(
                                o.source(), o.content(), service.actorName(o.recordedBy()), o.at()))
                        .toList(),
                wish.signedCopyFileId(),
                service.actorName(wish.recordedBy()),
                wish.recordedAt(),
                wish.status(),
                wish.verifiedBy() == null ? null : service.actorName(wish.verifiedBy()),
                wish.verifiedAt(),
                null);
    }

    /** Bản Nháp tự khai (Q-412, FR-023n); trường bắt buộc của phiên bản Hiệu lực có thể trống. */
    public static TreatmentWishBody ofDraft(TreatmentWishDraft draft, TreatmentWishService service) {
        TreatmentDraftContent c = draft.content();
        List<NotifyContact> contacts = service.notifyContacts(c.notifyFamilyMemberIds());
        return new TreatmentWishBody(
                draft.id(),
                c.preferredHospitals(),
                c.healthInsurance(),
                c.askFamilyFirst(),
                contacts.stream()
                        .map(n -> new NotifyFamilyMemberBody(
                                n.familyMemberId(),
                                n.fullName(),
                                service.relationshipOf(draft.residentId(), n.familyMemberId()),
                                n.phoneNumber()))
                        .toList(),
                c.accompaniedBy(),
                c.note(),
                c.respondent(),
                c.otherOpinions().stream()
                        .map(o -> new OtherOpinionBody(
                                o.source(), o.content(), service.actorName(draft.recordedBy()), draft.recordedAt()))
                        .toList(),
                c.signedCopyFileId(),
                service.actorName(draft.recordedBy()),
                draft.recordedAt(),
                TreatmentWishStatus.valueOf(draft.status()),
                null,
                null,
                DraftDiscardBody.of(draft.discard(), service::actorName));
    }
}
