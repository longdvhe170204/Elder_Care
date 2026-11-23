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
        List<Long> attachmentIds,
        Boolean includeWishes) {

    public TransferOutCommand {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }

    /** Cùng lệnh với thời điểm chuyển đã xác định (không gửi thì là giờ hệ thống lúc thực hiện). */
    public TransferOutCommand withTransferAt(Instant at) {
        return new TransferOutCommand(
                transferUnit,
                at,
                expectedReturnAt,
                handoverStaffId,
                companion,
                requiredContactReached,
                notAskedFirstReason,
                attachmentIds,
                includeWishes);
    }
}
