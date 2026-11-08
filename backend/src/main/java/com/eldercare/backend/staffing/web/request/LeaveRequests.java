package com.eldercare.backend.staffing.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Thân lệnh nghỉ đột xuất (hợp đồng {@code CreateLeave}, {@code LeaveDecision}). */
public final class LeaveRequests {
    private LeaveRequests() {}

    public record Create(
            @NotNull LeaveDetail.LeaveKind kind,
            Long staffId,
            List<Long> shiftIds,
            LocalDate fromDate,
            LocalDate toDate,
            List<LocalDate> dates,
            String scopeType,
            Long scopeId,
            @NotNull Reason reason) {}

    public record Item(Long shiftId, LocalDate date, @NotNull Result result, Long newLeadId) {}

    public enum Result {
        APPROVED,
        REJECTED
    }

    public record Decision(List<Item> items, String reason) {}
}
