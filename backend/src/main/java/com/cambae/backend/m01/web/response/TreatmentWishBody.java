package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.HealthInsurance;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.PreferredHospital;
import com.cambae.backend.m01.api.dto.TreatmentEscort;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWish;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWishStatus;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Một phiên bản phiếu nguyện vọng khi điều trị (contracts/api-001.yaml, schema {@code TreatmentWish}). */
@Schema(name = "TreatmentWish")
public record TreatmentWishBody(
        long id,
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        boolean askFamilyFirst,
        List<NotifyFamilyMemberBody> notifyFamilyMembers,
        TreatmentEscort accompaniedBy,
        String note,
        OpinionSource respondent,
        List<OtherOpinionBody> otherOpinions,
        Long signedCopyFileId,
        String recordedBy,
        Instant at,
        TreatmentWishStatus status) {

    /** {@code relationship} chưa có vì 012 chưa đưa nhãn quan hệ qua {@code FamilyRelationshipApi}. */
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
                        .map(c -> new NotifyFamilyMemberBody(c.familyMemberId(), c.fullName(), null, c.phoneNumber()))
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
                wish.status());
    }
}
