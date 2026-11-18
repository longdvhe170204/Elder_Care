package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentDraftContent;
import com.eldercare.backend.resident.web.request.RecordTreatmentWishBody.OtherOpinionBody;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thân lệnh lập, sửa bản Nháp tự khai của phiếu nguyện vọng khi điều trị (contracts/api-001.yaml, PUT
 * {@code /residents/{id}/treatment-wish-draft}; Q-412, FR-023n): mọi trường trừ người trả lời để trống được.
 */
@Schema(name = "TreatmentWishDraftBody")
public record DraftTreatmentWishBody(
        List<PreferredHospital> preferredHospitals,
        HealthInsurance healthInsurance,
        Boolean askFamilyFirst,
        List<Long> notifyFamilyMemberIds,
        TreatmentEscort accompaniedBy,
        String note,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionSource respondent,

        List<OtherOpinionBody> otherOpinions,
        Long signedCopyFileId) {

    public TreatmentDraftContent content() {
        return new TreatmentDraftContent(
                preferredHospitals == null ? List.of() : preferredHospitals,
                healthInsurance,
                askFamilyFirst,
                notifyFamilyMemberIds == null ? List.of() : notifyFamilyMemberIds,
                accompaniedBy,
                note,
                respondent,
                otherOpinions == null
                        ? List.of()
                        : otherOpinions.stream()
                                .map(o -> new WishOpinion(o.source(), o.content()))
                                .toList(),
                signedCopyFileId);
    }
}
