package com.eldercare.backend.staffing.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/** Phản hồi của đổi ca và nghỉ đột xuất (hợp đồng {@code CommandResult}, {@code CreatedLeaveRequests}). */
public final class SwapResponses {
    private SwapResponses() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandResult(
            List<Map<String, Object>> notTransferredAssignments, List<Map<String, Object>> effects) {}

    public record CreatedLeaveRequests(List<Long> requestIds) {}
}
