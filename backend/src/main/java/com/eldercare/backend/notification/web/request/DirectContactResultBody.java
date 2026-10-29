package com.eldercare.backend.notification.web.request;

import com.eldercare.backend.notification.internal.directcontact.service.DirectContactService.Result;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Thân {@code POST /direct-contacts/{id}/results} (FR-021, FR-031): Không liên hệ được bắt buộc lý do. */
public record DirectContactResultBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Result result,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime occurredAt,

        String reason,
        String note) {}
