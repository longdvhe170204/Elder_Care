package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService.Summary;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Phiên bản Hiệu lực và lịch sử (contracts/api-001.yaml, schema {@code TreatmentWishSummary}). Nội dung thuộc nhóm sức
 * khỏe nên chỉ ghi khi người xem được xem đầy đủ nhóm đó (FR-023m).
 */
@Schema(name = "TreatmentWishSummary")
public record TreatmentWishSummaryBody(
        @FieldGroup(InfoGroup.HEALTH) TreatmentWishBody active,
        @FieldGroup(InfoGroup.HEALTH) List<TreatmentWishBody> history,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static TreatmentWishSummaryBody of(long residentId, Summary summary, TreatmentWishService service) {
        return new TreatmentWishSummaryBody(
                summary.active().map(w -> TreatmentWishBody.of(w, service)).orElse(null),
                summary.history().stream()
                        .map(w -> TreatmentWishBody.of(w, service))
                        .toList(),
                List.of(),
                ResidentProfileService.target(residentId));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
