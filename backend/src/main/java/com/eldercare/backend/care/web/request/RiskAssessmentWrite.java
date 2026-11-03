package com.eldercare.backend.care.web.request;

import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nhận định khả năng tự quyết trong thỏa thuận rủi ro (schema {@code SelfDeterminationAssessment} của api-005, giống
 * api-001). Tên schema riêng để không đè schema cùng tên của 001 trong OpenAPI sinh ra; {@code at} do server ghi.
 */
@Schema(
        name = "CareSelfDeterminationAssessment",
        requiredProperties = {"canSelfDetermine", "supportMethod", "reviewDate"})
public record RiskAssessmentWrite(
        boolean canSelfDetermine,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Instant at,
        String supportMethod,
        LocalDate reviewDate) {

    SelfDeterminationAssessment toAssessment() {
        return new SelfDeterminationAssessment(canSelfDetermine, supportMethod, reviewDate);
    }
}
