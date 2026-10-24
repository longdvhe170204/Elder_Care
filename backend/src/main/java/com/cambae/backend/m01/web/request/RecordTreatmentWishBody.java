package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.api.dto.HealthInsurance;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.PreferredHospital;
import com.cambae.backend.m01.api.dto.TreatmentEscort;
import com.cambae.backend.m01.api.dto.WishOpinion;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService.RecordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thân lệnh ghi phiên bản phiếu nguyện vọng khi điều trị (contracts/api-001.yaml, POST
 * {@code /residents/{id}/treatment-wishes}; FR-023j, FR-023g). Danh sách bệnh viện không gắn {@code @Size} để thiếu hoặc
 * quá hai bệnh viện trả 422 với mã lỗi nghiệp vụ thay vì 400.
 */
public record RecordTreatmentWishBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<PreferredHospital> preferredHospitals,

        HealthInsurance healthInsurance,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean askFamilyFirst,

        @Schema(description = "Người thân có quan hệ Hiệu lực; 0..n")
        List<Long> notifyFamilyMemberIds,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        TreatmentEscort accompaniedBy,

        String note,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionSource respondent,

        @Schema(description = "Ý kiến gia đình khác ý kiến người cao tuổi, mỗi ý kiến một mục có nguồn (BR-M01-14)")
        List<OtherOpinionBody> otherOpinions,

        @Schema(description = "Không bắt buộc") Long signedCopyFileId) {

    @Schema(name = "RecordTreatmentWishOpinion")
    public record OtherOpinionBody(@NotNull OpinionSource source, String content) {}

    public RecordCommand command() {
        List<WishOpinion> opinions = otherOpinions == null
                ? List.of()
                : otherOpinions.stream()
                        .map(o -> new WishOpinion(o.source(), o.content()))
                        .toList();
        return new RecordCommand(
                preferredHospitals,
                healthInsurance,
                askFamilyFirst,
                notifyFamilyMemberIds == null ? List.of() : notifyFamilyMemberIds,
                accompaniedBy,
                note,
                respondent,
                opinions,
                signedCopyFileId);
    }
}
