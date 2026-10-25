package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Thân Cập nhật lượt đăng ký (contracts/api-004.yaml, POST {@code /admission-applications/{id}/update}): nội dung như
 * khi lập cộng lý do. {@code residentId} theo hợp đồng nhưng bị bỏ qua, lượt không đổi người cao tuổi (FR-001).
 */
public record UpdateAdmissionApplicationBody(
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate applicationDate,

        @Size(max = 200) String contactPerson,
        @Size(max = 2000) String needs,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ResidenceType desiredResidenceType,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        CareLevel expectedCareLevel,

        LocalDate desiredAdmissionDate,
        @Size(max = 2000) String note,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {

    public AdmissionApplication.Content content() {
        return new AdmissionApplication.Content(
                applicationDate,
                contactPerson,
                needs,
                desiredResidenceType,
                expectedCareLevel,
                desiredAdmissionDate,
                note);
    }
}
