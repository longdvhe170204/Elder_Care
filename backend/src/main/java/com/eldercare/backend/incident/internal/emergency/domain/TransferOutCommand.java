package com.eldercare.backend.incident.internal.emergency.domain;

import java.time.Instant;
import java.util.List;

/** Thân lệnh Chuyển viện từ sự cố (schema {@code TransferOut} của api-007; FR-049, Q-383, Q-384). */
public record TransferOutCommand(
        String transferUnit,
        Instant transferAt,
        Instant expectedReturnAt,
        Long handoverStaffId,
        String companion,
        Boolean requiredContactReached,
        String notAskedFirstReason,
        List<Long> attachmentIds) {

    public TransferOutCommand {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
}
