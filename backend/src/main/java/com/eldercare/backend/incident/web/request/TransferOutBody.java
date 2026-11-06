package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutCommand;
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
        @ArraySchema(maxItems = 10) List<Long> attachmentIds) {

    /** Lệnh Chuyển viện của dịch vụ; {@code null} khi thân lệnh không có khối này. */
    public static TransferOutCommand toCommand(TransferOutBody body) {
        if (body == null) return null;
        return new TransferOutCommand(
                body.transferUnit(),
                body.transferAt(),
                body.expectedReturnAt(),
                body.handoverStaffId(),
                body.companion(),
                body.requiredContactReached(),
                body.notAskedFirstReason() == null
                        ? null
                        : body.notAskedFirstReason().value(),
                body.attachmentIds());
    }
}
