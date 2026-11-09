package com.eldercare.backend.staffing.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.staffing.internal.swap.domain.SwapKind;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh đổi ca (hợp đồng {@code CreateShiftSwap}, {@code ShiftSwapCommand}). */
public final class SwapRequests {
    private SwapRequests() {}

    public record CreateShiftSwap(
            @NotNull SwapKind kind,
            @NotNull Long shiftXId,
            @NotNull Long recipientId,
            Long shiftYId,
            @NotNull Reason reason) {}

    public record ShiftSwapCommand(Reason reason, Long newLeadXId, Long newLeadYId, Boolean confirmWarning) {}
}
