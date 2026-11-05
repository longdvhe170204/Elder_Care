package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thân lệnh lập yêu cầu ngoại lệ đón (FR-024). */
public record PickupExceptionRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 40) String idDocumentNumber,
        @NotBlank @Size(max = 100) String relationship,
        @NotNull Reason reason) {}
