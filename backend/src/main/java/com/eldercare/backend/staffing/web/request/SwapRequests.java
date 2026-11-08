package com.eldercare.backend.staffing.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.staffing.internal.swap.domain.SwapKind;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh đổi ca (hợp đồng {@code CreateShiftSwap}, thân lệnh {@code /shift-swaps/{id}/{command}}). */
public final class SwapRequests {
    private SwapRequests() {}

    public record Create(
            @NotNull SwapKind kind, @NotNull Long shiftXId, @NotNull Long recipientId, Long shiftYId, @NotNull Reason reason) {}

    public record Command(Reason reason, Long newLeadXId, Long newLeadYId, Boolean confirmWarning) {}
}
