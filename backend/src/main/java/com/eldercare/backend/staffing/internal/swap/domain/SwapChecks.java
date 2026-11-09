package com.eldercare.backend.staffing.internal.swap.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Kết quả kiểm tra FR-021 (hợp đồng {@code ShiftSwapChecks}). Mục (g) chỉ có khi người xem là người duyệt; mục (f) tỷ lệ
 * phục vụ lấy từ 008 FR-034 khi có.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwapChecks(
        List<Map<String, Object>> overlaps,
        List<Map<String, Object>> continuousHours,
        List<Map<String, Object>> nonTransferableAssignments,
        Map<String, Object> leadAfterSwap,
        Map<String, Object> staffingRatioBeforeAfter,
        Map<String, Object> monthlyHours,
        List<Map<String, Object>> overlappingTrips) {

    public boolean blocked() {
        return !overlaps.isEmpty();
    }

    /** Người nhận chỉ thấy phần của mình: bỏ (g) và phần của người nhường không liên quan (FR-021). */
    public SwapChecks withoutApproverOnly() {
        return new SwapChecks(
                overlaps,
                continuousHours,
                nonTransferableAssignments,
                leadAfterSwap,
                staffingRatioBeforeAfter,
                null,
                overlappingTrips);
    }
}
