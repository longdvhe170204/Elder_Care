package com.cambae.backend.m01.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nhận định của Bác sĩ gắn với chính phiếu nguyện vọng cuối đời, không phải dấu năng lực của người cao tuổi
 * (contracts/api-001.yaml, schema {@code SelfDeterminationAssessment}; FR-023h). {@code at} do hệ thống đặt khi ghi
 * nên bị bỏ qua ở thân lệnh. Dùng chung cho thân lệnh và phản hồi để hai bên cùng một schema.
 */
@Schema(
        name = "SelfDeterminationAssessment",
        requiredProperties = {"canSelfDetermine", "supportMethod", "reviewDate"})
public record SelfDeterminationAssessmentBody(
        boolean canSelfDetermine,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Instant at,
        String supportMethod,
        LocalDate reviewDate) {}
