package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.administration.account.api.FieldGroup;
import com.eldercare.backend.administration.account.api.VisibilityTarget;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService.Summary;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Phiên bản Hiệu lực và lịch sử (contracts/api-001.yaml, schema {@code EndOfLifeWishSummary}). Nội dung thuộc nhóm
 * nguyện vọng nên chỉ ghi khi người xem được xem đầy đủ nhóm đó; {@code hasEndOfLifeWish} luôn có (FR-023e).
 */
@Schema(name = "EndOfLifeWishSummary")
public record EndOfLifeWishSummaryBody(
        boolean hasEndOfLifeWish,
        @FieldGroup(InfoGroup.WISHES) EndOfLifeWishBody current,
        @FieldGroup(InfoGroup.WISHES) List<EndOfLifeWishBody> history,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static EndOfLifeWishSummaryBody of(long residentId, Summary summary, EndOfLifeWishService service) {
        return new EndOfLifeWishSummaryBody(
                summary.hasWish(),
                summary.current().map(w -> EndOfLifeWishBody.of(w, service)).orElse(null),
                summary.history().stream()
                        .map(w -> EndOfLifeWishBody.of(w, service))
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
