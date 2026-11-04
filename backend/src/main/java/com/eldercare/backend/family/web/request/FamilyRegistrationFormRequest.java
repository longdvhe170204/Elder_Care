package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService.RegistrationFormLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Thân ghi phiếu đăng ký người thân (api-012 {@code FamilyRegistrationForm}; FR-008). */
@Schema(name = "FamilyRegistrationForm")
public record FamilyRegistrationFormRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long representativeId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long scanFileId,

        @NotEmpty @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Line> lines) {

    @Schema(name = "FamilyRegistrationFormLine")
    public record Line(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long relationshipId,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            FamilyRight right) {}

    public List<RegistrationFormLine> commandLines() {
        return lines.stream()
                .map(l -> new RegistrationFormLine(l.relationshipId(), l.right()))
                .toList();
    }
}
