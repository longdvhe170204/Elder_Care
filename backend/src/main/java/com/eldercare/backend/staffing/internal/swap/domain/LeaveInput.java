package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.common.Reason;
import java.time.LocalDate;
import java.util.List;

/**
 * Lệnh lập yêu cầu nghỉ (hợp đồng {@code CreateLeave}). Theo ca: {@code shiftIds} và/hoặc khoảng ngày {@code fromDate}
 * → {@code toDate}; theo ngày: {@code dates} cùng phạm vi {@code scopeType}, {@code scopeId} (FR-026a; chờ chủ dự án
 * chốt thêm hai trường phạm vi vào hợp đồng, xem tasks T024).
 */
public record LeaveInput(
        LeaveDetail.LeaveKind kind,
        Long staffId,
        List<Long> shiftIds,
        LocalDate fromDate,
        LocalDate toDate,
        List<LocalDate> dates,
        String scopeType,
        Long scopeId,
        Reason reason) {

    public LeaveInput {
        shiftIds = shiftIds == null ? List.of() : List.copyOf(shiftIds);
        dates = dates == null ? List.of() : List.copyOf(dates);
    }
}
