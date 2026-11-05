package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Thông tin Chuyển viện (schema {@code TransferOut} của api-007; FR-049, Q-383, Q-384). */
@Schema(
        name = "TransferOut",
        requiredProperties = {"transferUnit", "transferAt", "companion"})
public record TransferOutBody(
        String transferUnit,
        Instant transferAt,
        Instant expectedReturnAt,
        Long handoverStaffId,
        @Schema(minLength = 1) String companion,
        Boolean requiredContactReached,
        Reason notAskedFirstReason,
        @ArraySchema(maxItems = 10) List<Long> attachmentIds) {}
