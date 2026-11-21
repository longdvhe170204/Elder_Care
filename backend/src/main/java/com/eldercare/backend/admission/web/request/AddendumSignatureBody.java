package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Ghi nhận phụ lục đã ký (api-004 {@code AddendumSignatureInput}; FR-043d, Q-419). */
@Schema(name = "AddendumSignatureInput")
public record AddendumSignatureBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate signedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long signedByFamilyMemberId,
        Long facilitySignatoryStaffId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long fileId,
        @Schema(maxLength = 1000) String note) {

    public ResidencyChangeService.SignatureInput toInput() {
        return new ResidencyChangeService.SignatureInput(
                signedDate, signedByFamilyMemberId, facilitySignatoryStaffId, fileId, note);
    }
}
